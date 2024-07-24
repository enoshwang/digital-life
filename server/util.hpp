#ifndef UTIL_HPP
#define UTIL_HPP

#include <string>
#include <arpa/inet.h>

namespace wd {
namespace util {

std::string getSockAddrInfo(const struct sockaddr_in &sockAddr) noexcept;
}  // namespace util

}  // namespace wd

#endif  // UTIL_HPP
