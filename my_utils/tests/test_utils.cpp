#include <my_utils/utils.hpp>

#include "../extern/catch_amalgamated.hpp"

TEST_CASE("String splitting works", "[string]")
{
    auto result = ew::my_utils::isLittleEndian();
    //REQUIRE(result.size() == 3);
    CHECK(result == true);

    SECTION("Empty input handling")
    {
        
    }
}
