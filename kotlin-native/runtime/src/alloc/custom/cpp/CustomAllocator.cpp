/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "CustomAllocator.hpp"

#include <cstdint>
#include <cinttypes>
#include <cstring>
#include <new>

#include "CustomLogging.hpp"
#include "ExtraObjectData.hpp"
#include "KAssert.h"
#include "NextFitPage.hpp"
#include "Memory.h"
#include "FixedBlockPage.hpp"
#include "GCApi.hpp"

namespace kotlin::alloc {

CustomAllocator::CustomAllocator(Heap& heap) noexcept : heap_(heap), nextFitPage_(nullptr), extraObjectPage_(nullptr) {
    CustomAllocInfo("CustomAllocator::CustomAllocator(heap)");
    memset(fixedBlockPages_, 0, sizeof(fixedBlockPages_));
}

CustomAllocator::~CustomAllocator() {
    heap_.AddToFinalizerQueue(std::move(finalizerQueue_));
}

ObjHeader* CustomAllocator::CreateObject(const TypeInfo* typeInfo) noexcept {
    RuntimeAssert(!typeInfo->IsArray(), "Must not be an array");
    auto descriptor = CustomHeapObject::descriptorFrom(typeInfo);
    auto size = AllocationSize::bytesAtLeast(descriptor.size());
    auto& heapObject = *descriptor.construct(Allocate(size));
    ObjHeader* object = heapObject.object();
    if (typeInfo->flags_ & TF_HAS_FINALIZER) {
        auto* extraObject = CreateExtraObjectDataForObject(object, typeInfo);
        object->typeInfoOrMeta_ = reinterpret_cast<TypeInfo*>(extraObject);
        CustomAllocDebug("CustomAllocator: %p gets extraObject %p", object, extraObject);
        CustomAllocDebug("CustomAllocator: %p->BaseObject == %p", extraObject, extraObject->GetBaseObject());
    } else {
        object->typeInfoOrMeta_ = const_cast<TypeInfo*>(typeInfo);
    }
    return object;
}

ArrayHeader* CustomAllocator::CreateArray(const TypeInfo* typeInfo, uint32_t count) noexcept {
    CustomAllocDebug("CustomAllocator@%p::CreateArray(%d)", this ,count);
    RuntimeAssert(typeInfo->IsArray(), "Must be an array");
    auto descriptor = CustomHeapArray::descriptorFrom(typeInfo, count);
    auto size = AllocationSize::bytesAtLeast(descriptor.size());
    auto& heapArray = *descriptor.construct(Allocate(size));
    ArrayHeader* array = heapArray.array();
    array->typeInfoOrMeta_ = const_cast<TypeInfo*>(typeInfo);
    array->count_ = count;
    return array;
}

mm::ExtraObjectData* CustomAllocator::CreateExtraObjectDataForObject(ObjHeader* baseObject, const TypeInfo* type) noexcept {
    CustomAllocDebug("CustomAllocator@%p::CreateExtraObjectDataForObject(%p)", this ,baseObject);
    auto* extraObjectCell = reinterpret_cast<ExtraObjectCell*>(AllocateExtraObjectData());
    return new (extraObjectCell->data_) mm::ExtraObjectData(baseObject, type);
}

FinalizerQueue CustomAllocator::ExtractFinalizerQueue() noexcept {
    return std::move(finalizerQueue_);
}

void CustomAllocator::PrepareForGC() noexcept {
    CustomAllocInfo("CustomAllocator@%p::PrepareForGC()", this);
    nextFitPage_ = nullptr;
    memset(fixedBlockPages_, 0, sizeof(fixedBlockPages_));
    extraObjectPage_ = nullptr;
}

// static
size_t CustomAllocator::GetAllocatedHeapSize(ObjHeader* object) noexcept {
    return CustomHeapObject::from(object).size();
}

uint8_t* CustomAllocator::Allocate(AllocationSize size) noexcept {
    RuntimeAssert(size > AllocationSize::cells(0), "CustomAllocator::Allocate cannot allocate 0 bytes");
    CustomAllocDebug("CustomAllocator::Allocate(%" PRIu64 ")", size.inBytes());
    uint64_t cellCount = size.inCells();
    if (!compiler::pagedAllocator()) {
        return AllocateInSingleObjectPage(cellCount);
    }
    if (cellCount <= FixedBlockPage::MAX_BLOCK_SIZE) {
        return AllocateInFixedBlockPage(cellCount);
    }
    if (cellCount > NextFitPage::maxBlockSize()) {
        return AllocateInSingleObjectPage(cellCount);
    }
    return AllocateInNextFitPage(cellCount);
}

uint8_t* CustomAllocator::AllocateInSingleObjectPage(uint64_t cellCount) noexcept {
    CustomAllocDebug("CustomAllocator::AllocateInSingleObjectPage(%" PRIu64 ")", cellCount);
    uint8_t* block = heap_.GetSingleObjectPage(cellCount)->Allocate();
    return block;
}

uint8_t* CustomAllocator::AllocateInNextFitPage(uint32_t cellCount) noexcept {
    CustomAllocDebug("CustomAllocator::AllocateInNextFitPage(%u)", cellCount);
    if (nextFitPage_) {
        uint8_t* block = nextFitPage_->TryAllocate(cellCount);
        if (block) return block;
    }
    CustomAllocDebug("Failed to allocate in curPage");
    while (true) {
        nextFitPage_ = heap_.GetNextFitPage(cellCount, finalizerQueue_);
        uint8_t* block = nextFitPage_->TryAllocate(cellCount);
        if (block) return block;
    }
}

uint8_t* CustomAllocator::AllocateInFixedBlockPage(uint32_t cellCount) noexcept {
    CustomAllocDebug("CustomAllocator::AllocateInFixedBlockPage(%u)", cellCount);
    FixedBlockPage* page = fixedBlockPages_[cellCount];
    if (page) {
        uint8_t* block = page->TryAllocate();
        if (block) return block;
    }
    CustomAllocDebug("Failed to allocate in current FixedBlockPage");
    while ((page = heap_.GetFixedBlockPage(cellCount, finalizerQueue_))) {
        uint8_t* block = page->TryAllocate();
        if (block) {
            fixedBlockPages_[cellCount] = page;
            return block;
        }
    }
    return nullptr;
}

uint8_t* CustomAllocator::AllocateExtraObjectData() noexcept {
    CustomAllocDebug("CustomAllocator::AllocateExtraObjectData()");

    if (!compiler::pagedAllocator()) {
        return heap_.GetSingleExtraObjectPage()->Allocate();
    }

    auto* page = extraObjectPage_;
    if (page) {
        uint8_t* block = page->TryAllocate();
        if (block) return block;
    }

    CustomAllocDebug("Failed to allocate in current ExtraObjectPage");
    while ((page = heap_.GetFixedBlockExtraObjectPage(finalizerQueue_))) {
        uint8_t* block = page->TryAllocate();
        if (block) {
            extraObjectPage_ = page;
            return block;
        }
    }
    return nullptr;
}

} // namespace kotlin::alloc
