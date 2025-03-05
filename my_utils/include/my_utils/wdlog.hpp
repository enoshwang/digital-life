#pragma once

#include <iostream>
#include <source_location>

namespace wd {
namespace common {

template <typename... Args>
class Log
{
public:
    Log(Args... args,
        const std::source_location location = std::source_location::current())
    {
        std::cout << location.file_name() << ":" << location.line() << ":"
                  << location.column() << " " << location.function_name()
                  << ": ";

        ((std::cout << args << " "), ...) << std::endl;
    }
};

// C++17 deduction guides :
// https://en.cppreference.com/w/cpp/language/class_template_argument_deduction
// TODO ：string str{"nihao"}; Log(str); why happend error C2371: “str”:
// 重定义；不同的基类型
template <typename... Args>
Log(Args...) -> Log<Args...>;

}  // namespace common
}  // namespace wd