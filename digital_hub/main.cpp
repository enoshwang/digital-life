#define SPDLOG_ACTIVE_LEVEL SPDLOG_LEVEL_TRACE
#include <spdlog/sinks/rotating_file_sink.h>
#include <spdlog/spdlog.h>
#include <asio/asio.hpp>

#include <QApplication>
#include <QIcon>

#include <chrono>
#include <iostream>
#include <vector>
#include <string>

#include <QUuid>

#include "mainwindow.h"
#include "login.h"
#include "quihelper.h"

using namespace std::string_literals;

void init_log();

std::vector<std::string> g_argv{};

int main(int argc, char *argv[])
{
    init_log();
    SPDLOG_INFO("main start");

    bool is_enable_login = false;

    g_argv.reserve(32);
    for(int i{0};i<argc;++i)
    {
        g_argv.emplace_back(argv[i]);
    }

    std::string program_argv {""s};
    for(auto i{0};i<g_argv.size();++i)
    {
        program_argv += g_argv[i];
        if(i != (g_argv.size() - 1))
        {
            program_argv += " "s;
        }
    }
    SPDLOG_INFO("argc:{0},argv:{1}",argc,program_argv);

    QApplication app(argc, argv);
    app.setWindowIcon(QIcon(":/main.ico"));

    QUIHelper::init();

    if(is_enable_login)
    {
        Login l;
        l.show();

        if(l.exec() == QDialog::Accepted)
        {
            MainWindow   mw;
            mw.show();
            SPDLOG_DEBUG("mw show");

            return app.exec();
        }
    }
    else
    {
        QUIHelper::m_username = QUuid::createUuid().toString(QUuid::StringFormat::WithoutBraces).toStdString();

        MainWindow   mw;
        mw.show();
        SPDLOG_DEBUG("mw show");


        return app.exec();
    }

    return 0;
}

void init_log()
{
    try {
        // rotating_sink : 5mb per file, 0 rotated files
        auto file_name = "wdChat.log"s;
        auto max_size  = 1048576 * 5;
        auto max_files = 0;
        auto rotating_sink =
            std::make_shared<spdlog::sinks::rotating_file_sink_mt>(
                file_name, max_size, max_files);

        // logger : rotating_sink
        auto logger = std::make_shared<spdlog::logger>("logger", rotating_sink);
        int64_t now = std::chrono::duration_cast<std::chrono::milliseconds>(
                          std::chrono::system_clock::now().time_since_epoch()).count();
        std::string spdlog_pattern =
            "[%Y-%m-%d %H:%M:%S.%f] [%P] [%t] [%s:%#] [%l] "s + std::to_string(now) + " %v"s;
        logger->set_pattern(spdlog_pattern);
        logger->set_level(spdlog::level::trace);
        logger->flush_on(spdlog::level::info);

        spdlog::set_default_logger(logger);
    } catch (const spdlog::spdlog_ex &ex) {
        std::cerr << "Log init failed: " << ex.what() << std::endl;
        exit(1);
    }
}
