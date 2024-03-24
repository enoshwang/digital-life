#include "util.hpp"

namespace wd {
namespace util {

std::string getSockAddrInfo(const struct sockaddr_in &sockAddr) noexcept
{
    std::string ip(inet_ntoa(sockAddr.sin_addr));
    std::string port(std::to_string(ntohs(sockAddr.sin_port)));
    return ip + ":" + port;
}

}  // namespace util
}  // namespace wd
