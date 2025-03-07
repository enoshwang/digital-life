#include <my_utils/utils.hpp>
#include <iostream>

int main() 
{
    bool isLittleEndian = ew::my_utils::isLittleEndian();
    std::cout << "isLittleEndian: " << isLittleEndian << std::endl;

    ew::my_utils::Errno err = ew::my_utils::Errno::SUCCESS;
    std::cout << "err: " << ew::my_utils::ERRNO_MSG.at(err) << std::endl;

    std::cout << ew::my_utils::FileSystem::isExist("C://", true) << std::endl;

    return 0;
}
