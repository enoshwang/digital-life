#include <iostream>
#include <map>
#include <string>
#include <vector>
#include <wdcommon.hpp>


using namespace std;

void test_wd_common_uuid();
void test_wd_common_isLittleEndian();

int main(int argc, char* argv[])
{
    vector testList = {"test_wd_common_uuid", "test_wd_common_isLittleEndian"};

    map<string, function<void()>> testMap = {
        {"test_wd_common_uuid", test_wd_common_uuid},
        {"test_wd_common_isLittleEndian", test_wd_common_isLittleEndian}};

    for (const auto& test : testList) {
        testMap[test]();
    }
}

void test_wd_common_uuid()
{
    wd::common::Log("test uuid");
    wd::common::Log(wd::common::generate_uuid_v4().c_str());
}

void test_wd_common_isLittleEndian()
{
    wd::common::Log("test isLittleEndian");
    auto ret = wd::common::isLittleEndian();
    wd::common::Log("isLittleEndian:",ret);
}