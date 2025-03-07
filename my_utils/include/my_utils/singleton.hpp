#pragma once

#include <memory>
#include <mutex>

namespace ew {
namespace my_utils {

template <typename T>
class Singleton final
{
public:
    template <typename... Args>
    static std::shared_ptr<T> GetInstance(Args &&...args)
    {
        static std::shared_ptr<T> m_pSington = nullptr;
        static std::mutex         m_Mutex;

        if (!m_pSington) [[unlikely]] {
            std::lock_guard<std::mutex> gLock(m_Mutex);
            if (nullptr == m_pSington) {
                m_pSington = std::make_shared<T>(std::forward<Args>(args)...);
            }
        }
        return m_pSington;
    }

private:
    Singleton()                               = default;
    Singleton(const Singleton &)            = delete;
    Singleton &operator=(const Singleton &) = delete;
    Singleton(Singleton &&)                 = delete;
    Singleton &operator=(Singleton &&)      = delete;
    ~Singleton()                              = default;
};

}  // namespace my_utils
}  // namespace ew