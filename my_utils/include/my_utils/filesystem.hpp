#pragma once

#include <filesystem>

namespace ew {
namespace my_utils {

class FileSystem
{
public:
    FileSystem(){}

    static bool isExist(const std::filesystem::path& path,bool isDirectory) noexcept;
};

}  // namespace my_utils
}  // namespace ew