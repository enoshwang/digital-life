#pragma once

#include <condition_variable>
#include <mutex>
#include <queue>

namespace wd {
namespace common {

template <typename T>
class EWQueue
{
public:
    EWQueue()
    {
    }

    void push(T value)
    {
        std::unique_lock<std::mutex> lock(m_mutex);
        m_queue.push(std::move(value));
        lock.unlock();
        m_condition.notify_one();
    }

    bool try_pop(T& value)
    {
        std::lock_guard<std::mutex> lock(m_mutex);
        if (m_queue.empty()) {
            return false;
        }
        value = std::move(m_queue.front());
        m_queue.pop();
        return true;
    }

    void wait_and_pop(T& value)
    {
        std::unique_lock<std::mutex> lock(m_mutex);
        m_condition.wait(lock, [this]() { return !m_queue.empty(); });
        value = std::move(m_queue.front());
        m_queue.pop();
    }

    bool empty() const
    {
        std::lock_guard<std::mutex> lock(m_mutex);
        return m_queue.empty();
    }

    size_t size() const
    {
        std::lock_guard<std::mutex> lock(m_mutex);
        return m_queue.size();
    }

private:
    mutable std::mutex      m_mutex;
    std::queue<T>           m_queue{};
    std::condition_variable m_condition{};
};

}  // namespace common
}  // namespace wd