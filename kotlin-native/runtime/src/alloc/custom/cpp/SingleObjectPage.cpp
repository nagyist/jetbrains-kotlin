/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "SingleObjectPage.hpp"

#include <atomic>
#include <cstdint>

#include "AllocatorImpl.hpp"
#include "CustomLogging.hpp"
#include "GCApi.hpp"
#include "NextFitPage.hpp"

namespace kotlin::alloc {

SingleObjectPage* SingleObjectPage::Create(uint64_t cellCount) noexcept {
    CustomAllocInfo("SingleObjectPage::Create(%" PRIu64 ")", cellCount);
    RuntimeAssert(!compiler::pagedAllocator() || cellCount > NextFitPage::maxBlockSize(), "blockSize too small for SingleObjectPage");
    auto objectSize = AllocationSize::cells(cellCount);
    return new (SafeAlloc(pageSize(objectSize).inBytes())) SingleObjectPage(objectSize);
}

SingleObjectPage::SingleObjectPage(AllocationSize objectSize) noexcept {
    auto& heap = mm::GlobalData::Instance().allocator().impl().heap();
    heap.allocatedSizeTracker().recordDifferenceAndNotifyScheduler(static_cast<ptrdiff_t>(objectSize.inBytes()));
}

uint8_t* SingleObjectPage::Data() noexcept {
    return data_;
}

uint8_t* SingleObjectPage::Allocate() noexcept {
    return Data();
}

AllocationSize SingleObjectPage::pageSize(AllocationSize objectSize) noexcept {
    return objectSize + AllocationSize::bytesAtLeast(sizeof(SingleObjectPage));
}

std::vector<uint8_t*> SingleObjectPage::GetAllocatedBlocks() noexcept {
    std::vector<uint8_t*> allocated;
    TraverseAllocatedBlocks([&allocated](uint8_t* block) {
        allocated.push_back(block);
    });
    return allocated;
}

void SingleObjectPage::destroyImpl(AllocationSize objectSize) noexcept {
    auto& heap = mm::GlobalData::Instance().allocator().impl().heap();
    heap.allocatedSizeTracker().recordDifference(-static_cast<ptrdiff_t>(objectSize.inBytes()));

    Free(this, pageSize(objectSize).inBytes());
}

} // namespace kotlin::alloc
