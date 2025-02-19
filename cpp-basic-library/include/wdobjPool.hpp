#pragma once

#include <deque>
#include <functional>
#include <memory>
#include <mutex>

namespace wd {
namespace common {

class EWObjectPoolBase
{
protected:
    EWObjectPoolBase(/* args */)                          = default;
    virtual ~EWObjectPoolBase()                           = default;
    EWObjectPoolBase(const EWObjectPoolBase &)            = default;
    EWObjectPoolBase &operator=(const EWObjectPoolBase &) = default;
    EWObjectPoolBase(EWObjectPoolBase &&)                 = default;
    EWObjectPoolBase &operator=(EWObjectPoolBase &&)      = default;
};

template <typename T, size_t N>
class EWObjectPool final : public EWObjectPoolBase
{
public:
    using DeleterType = std::function<void(T *)>;

public:
    std::unique_ptr<T, DeleterType> acquire()
    {
        // TODO 完善锁机制
        std::unique_lock<std::mutex> lock;
        if (empty()) {
            return nullptr;
        }

        std::unique_ptr<T, DeleterType> ptr(
            _pool.front().release(),
            [this](T *t) { _pool.push_back(std::unique_ptr<T>(t)); });

        _pool.pop_front();
        return ptr;
    }

    bool empty() const
    {
        return _pool.empty();
    }

    size_t size() const
    {
        return _pool.size();
    }

public:
    EWObjectPool()
    {
        static_assert(N < 500, "init pool size is out of range");
        for (size_t i{0}; i < N; ++i) {
            _pool.push_back(std::make_unique<T>());
        }
    }
    ~EWObjectPool() = default;

private:
    std::deque<std::unique_ptr<T>> _pool;
    static std::mutex              _Mutex;
};

}  // namespace common
}  // namespace wd