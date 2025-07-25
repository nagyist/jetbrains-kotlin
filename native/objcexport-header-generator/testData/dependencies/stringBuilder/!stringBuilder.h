#import <Foundation/NSArray.h>
#import <Foundation/NSDictionary.h>
#import <Foundation/NSError.h>
#import <Foundation/NSObject.h>
#import <Foundation/NSSet.h>
#import <Foundation/NSString.h>
#import <Foundation/NSValue.h>

@class KotlinCharArray, KotlinCharIterator, KotlinStringBuilder;

@protocol KotlinAppendable, KotlinCharSequence, KotlinIterator;

NS_ASSUME_NONNULL_BEGIN
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunknown-warning-option"
#pragma clang diagnostic ignored "-Wincompatible-property-type"
#pragma clang diagnostic ignored "-Wnullability"

#pragma push_macro("_Nullable_result")
#if !__has_feature(nullability_nullable_result)
#undef _Nullable_result
#define _Nullable_result _Nullable
#endif

__attribute__((objc_subclassing_restricted))
@interface FooKt : Base
@property (class, readonly) KotlinStringBuilder *a __attribute__((swift_name("a")));
@end

@protocol KotlinCharSequence
@required
- (unichar)getIndex:(int32_t)index __attribute__((swift_name("get(index:)")));
- (id)subSequenceStartIndex:(int32_t)startIndex endIndex:(int32_t)endIndex __attribute__((swift_name("subSequence(startIndex:endIndex:)")));
@property (readonly) int32_t length __attribute__((swift_name("length")));
@end

@protocol KotlinAppendable
@required

/**
 * @note annotations
 *   kotlin.IgnorableReturnValue
*/
- (id<KotlinAppendable>)appendValue:(unichar)value __attribute__((swift_name("append(value:)")));

/**
 * @note annotations
 *   kotlin.IgnorableReturnValue
*/
- (id<KotlinAppendable>)appendValue_:(id _Nullable)value __attribute__((swift_name("append(value_:)")));

/**
 * @note annotations
 *   kotlin.IgnorableReturnValue
*/
- (id<KotlinAppendable>)appendValue:(id _Nullable)value startIndex:(int32_t)startIndex endIndex:(int32_t)endIndex __attribute__((swift_name("append(value:startIndex:endIndex:)")));
@end

__attribute__((objc_subclassing_restricted))
@interface KotlinStringBuilder : Base <KotlinCharSequence, KotlinAppendable>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithContent:(id)content __attribute__((swift_name("init(content:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithCapacity:(int32_t)capacity __attribute__((swift_name("init(capacity:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithContent_:(NSString *)content __attribute__((swift_name("init(content_:)"))) __attribute__((objc_designated_initializer));

/**
 * @note annotations
 *   kotlin.IgnorableReturnValue
*/
- (KotlinStringBuilder *)appendValue__:(id _Nullable)value __attribute__((swift_name("append(value__:)")));

/**
 * @note annotations
 *   kotlin.IgnorableReturnValue
*/
- (KotlinStringBuilder *)appendValue___:(BOOL)value __attribute__((swift_name("append(value___:)")));
- (KotlinStringBuilder *)appendValue____:(int8_t)value __attribute__((swift_name("append(value____:)")));

/**
 * @note annotations
 *   kotlin.IgnorableReturnValue
*/
- (KotlinStringBuilder *)appendValue:(unichar)value __attribute__((swift_name("append(value:)")));

/**
 * @note annotations
 *   kotlin.IgnorableReturnValue
*/
- (KotlinStringBuilder *)appendValue_____:(KotlinCharArray *)value __attribute__((swift_name("append(value_____:)")));

/**
 * @note annotations
 *   kotlin.IgnorableReturnValue
*/
- (KotlinStringBuilder *)appendValue_:(id _Nullable)value __attribute__((swift_name("append(value_:)")));

/**
 * @note annotations
 *   kotlin.IgnorableReturnValue
*/
- (KotlinStringBuilder *)appendValue______:(double)value __attribute__((swift_name("append(value______:)")));

/**
 * @note annotations
 *   kotlin.IgnorableReturnValue
*/
- (KotlinStringBuilder *)appendValue_______:(float)value __attribute__((swift_name("append(value_______:)")));

/**
 * @note annotations
 *   kotlin.IgnorableReturnValue
*/
- (KotlinStringBuilder *)appendValue________:(int32_t)value __attribute__((swift_name("append(value________:)")));

/**
 * @note annotations
 *   kotlin.IgnorableReturnValue
*/
- (KotlinStringBuilder *)appendValue_________:(int64_t)value __attribute__((swift_name("append(value_________:)")));
- (KotlinStringBuilder *)appendValue__________:(int16_t)value __attribute__((swift_name("append(value__________:)")));

/**
 * @note annotations
 *   kotlin.IgnorableReturnValue
*/
- (KotlinStringBuilder *)appendValue___________:(NSString * _Nullable)value __attribute__((swift_name("append(value___________:)")));

/**
 * @note annotations
 *   kotlin.IgnorableReturnValue
*/
- (KotlinStringBuilder *)appendValue:(id _Nullable)value startIndex:(int32_t)startIndex endIndex:(int32_t)endIndex __attribute__((swift_name("append(value:startIndex:endIndex:)")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.4")
*/
- (KotlinStringBuilder *)appendRangeValue:(KotlinCharArray *)value startIndex:(int32_t)startIndex endIndex:(int32_t)endIndex __attribute__((swift_name("appendRange(value:startIndex:endIndex:)")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.4")
*/
- (KotlinStringBuilder *)appendRangeValue:(id)value startIndex:(int32_t)startIndex endIndex_:(int32_t)endIndex __attribute__((swift_name("appendRange(value:startIndex:endIndex_:)")));
- (int32_t)capacity __attribute__((swift_name("capacity()")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.4")
*/
- (KotlinStringBuilder *)deleteAtIndex:(int32_t)index __attribute__((swift_name("deleteAt(index:)")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.4")
*/
- (KotlinStringBuilder *)deleteRangeStartIndex:(int32_t)startIndex endIndex:(int32_t)endIndex __attribute__((swift_name("deleteRange(startIndex:endIndex:)")));
- (void)ensureCapacityMinimumCapacity:(int32_t)minimumCapacity __attribute__((swift_name("ensureCapacity(minimumCapacity:)")));
- (unichar)getIndex:(int32_t)index __attribute__((swift_name("get(index:)")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.4")
*/
- (int32_t)indexOfString:(NSString *)string __attribute__((swift_name("indexOf(string:)")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.4")
*/
- (int32_t)indexOfString:(NSString *)string startIndex:(int32_t)startIndex __attribute__((swift_name("indexOf(string:startIndex:)")));

/**
 * @note annotations
 *   kotlin.IgnorableReturnValue
*/
- (KotlinStringBuilder *)insertIndex:(int32_t)index value:(id _Nullable)value __attribute__((swift_name("insert(index:value:)")));

/**
 * @note annotations
 *   kotlin.IgnorableReturnValue
*/
- (KotlinStringBuilder *)insertIndex:(int32_t)index value_:(BOOL)value __attribute__((swift_name("insert(index:value_:)")));
- (KotlinStringBuilder *)insertIndex:(int32_t)index value__:(int8_t)value __attribute__((swift_name("insert(index:value__:)")));

/**
 * @note annotations
 *   kotlin.IgnorableReturnValue
*/
- (KotlinStringBuilder *)insertIndex:(int32_t)index value___:(unichar)value __attribute__((swift_name("insert(index:value___:)")));

/**
 * @note annotations
 *   kotlin.IgnorableReturnValue
*/
- (KotlinStringBuilder *)insertIndex:(int32_t)index value____:(KotlinCharArray *)value __attribute__((swift_name("insert(index:value____:)")));

/**
 * @note annotations
 *   kotlin.IgnorableReturnValue
*/
- (KotlinStringBuilder *)insertIndex:(int32_t)index value_____:(id _Nullable)value __attribute__((swift_name("insert(index:value_____:)")));

/**
 * @note annotations
 *   kotlin.IgnorableReturnValue
*/
- (KotlinStringBuilder *)insertIndex:(int32_t)index value______:(double)value __attribute__((swift_name("insert(index:value______:)")));

/**
 * @note annotations
 *   kotlin.IgnorableReturnValue
*/
- (KotlinStringBuilder *)insertIndex:(int32_t)index value_______:(float)value __attribute__((swift_name("insert(index:value_______:)")));

/**
 * @note annotations
 *   kotlin.IgnorableReturnValue
*/
- (KotlinStringBuilder *)insertIndex:(int32_t)index value________:(int32_t)value __attribute__((swift_name("insert(index:value________:)")));

/**
 * @note annotations
 *   kotlin.IgnorableReturnValue
*/
- (KotlinStringBuilder *)insertIndex:(int32_t)index value_________:(int64_t)value __attribute__((swift_name("insert(index:value_________:)")));
- (KotlinStringBuilder *)insertIndex:(int32_t)index value__________:(int16_t)value __attribute__((swift_name("insert(index:value__________:)")));

/**
 * @note annotations
 *   kotlin.IgnorableReturnValue
*/
- (KotlinStringBuilder *)insertIndex:(int32_t)index value___________:(NSString * _Nullable)value __attribute__((swift_name("insert(index:value___________:)")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.4")
*/
- (KotlinStringBuilder *)insertRangeIndex:(int32_t)index value:(KotlinCharArray *)value startIndex:(int32_t)startIndex endIndex:(int32_t)endIndex __attribute__((swift_name("insertRange(index:value:startIndex:endIndex:)")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.4")
*/
- (KotlinStringBuilder *)insertRangeIndex:(int32_t)index value:(id)value startIndex:(int32_t)startIndex endIndex_:(int32_t)endIndex __attribute__((swift_name("insertRange(index:value:startIndex:endIndex_:)")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.4")
*/
- (int32_t)lastIndexOfString:(NSString *)string __attribute__((swift_name("lastIndexOf(string:)")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.4")
*/
- (int32_t)lastIndexOfString:(NSString *)string startIndex:(int32_t)startIndex __attribute__((swift_name("lastIndexOf(string:startIndex:)")));

/**
 * @note annotations
 *   kotlin.IgnorableReturnValue
*/
- (KotlinStringBuilder *)reverse __attribute__((swift_name("reverse()")));
- (void)setIndex:(int32_t)index value:(unichar)value __attribute__((swift_name("set(index:value:)")));
- (void)setLengthNewLength:(int32_t)newLength __attribute__((swift_name("setLength(newLength:)")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.4")
*/
- (KotlinStringBuilder *)setRangeStartIndex:(int32_t)startIndex endIndex:(int32_t)endIndex value:(NSString *)value __attribute__((swift_name("setRange(startIndex:endIndex:value:)")));
- (id)subSequenceStartIndex:(int32_t)startIndex endIndex:(int32_t)endIndex __attribute__((swift_name("subSequence(startIndex:endIndex:)")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.4")
*/
- (NSString *)substringStartIndex:(int32_t)startIndex __attribute__((swift_name("substring(startIndex:)")));
- (NSString *)substringStartIndex:(int32_t)startIndex endIndex:(int32_t)endIndex __attribute__((swift_name("substring(startIndex:endIndex:)")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.4")
*/
- (void)toCharArrayDestination:(KotlinCharArray *)destination destinationOffset:(int32_t)destinationOffset startIndex:(int32_t)startIndex endIndex:(int32_t)endIndex __attribute__((swift_name("toCharArray(destination:destinationOffset:startIndex:endIndex:)")));
- (NSString *)description __attribute__((swift_name("description()")));
- (void)trimToSize __attribute__((swift_name("trimToSize()")));
@property (readonly) int32_t length __attribute__((swift_name("length")));
@end

__attribute__((objc_subclassing_restricted))
@interface KotlinCharArray : Base
+ (instancetype)arrayWithSize:(int32_t)size __attribute__((swift_name("init(size:)")));
+ (instancetype)arrayWithSize:(int32_t)size init:(id (^)(Int *))init __attribute__((swift_name("init(size:init:)")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (unichar)getIndex:(int32_t)index __attribute__((swift_name("get(index:)")));
- (KotlinCharIterator *)iterator __attribute__((swift_name("iterator()")));
- (void)setIndex:(int32_t)index value:(unichar)value __attribute__((swift_name("set(index:value:)")));
@property (readonly) int32_t size __attribute__((swift_name("size")));
@end

@protocol KotlinIterator
@required
- (BOOL)hasNext __attribute__((swift_name("hasNext()")));
- (id _Nullable)next __attribute__((swift_name("next()")));
@end

@interface KotlinCharIterator : Base <KotlinIterator>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (id)next __attribute__((swift_name("next()")));
- (unichar)nextChar __attribute__((swift_name("nextChar()")));
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
