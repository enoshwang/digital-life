#pragma once

#include <filesystem>

namespace wd {
namespace common {

class EWFileSystem
{
public:
    EWFileSystem()
    {
    }

    static bool isExist(const std::filesystem::path& path,
                        bool                         isDirectory) noexcept
    {
        std::error_code errCode;
        auto            fileStatus = std::filesystem::status(path, errCode);

        if (errCode)
            return false;

        if (!std::filesystem::exists(fileStatus))
            return false;

        if (std::filesystem::is_directory(fileStatus) && isDirectory)
            return true;

        if (!std::filesystem::is_directory(fileStatus) && !isDirectory)
            return true;

        return false;
    }
};

}  // namespace common
}  // namespace wd