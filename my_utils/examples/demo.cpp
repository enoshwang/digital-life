#include <my_utils/utils.hpp>
#include <iostream>

int main() 
{
    bool isLittleEndian = ew::my_utils::isLittleEndian();
    std::cout << "isLittleEndian: " << isLittleEndian << std::endl;

    return 0;
}
