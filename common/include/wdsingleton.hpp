#pragma once

#include <memory>
#include <mutex>

namespace wd {
namespace common {

template <typename T>
class EWSingleton final
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
    EWSingleton()                               = default;
    EWSingleton(const EWSingleton &)            = delete;
    EWSingleton &operator=(const EWSingleton &) = delete;
    EWSingleton(EWSingleton &&)                 = delete;
    EWSingleton &operator=(EWSingleton &&)      = delete;
    ~EWSingleton()                              = default;
};

}  // namespace common
}  // namespace wd