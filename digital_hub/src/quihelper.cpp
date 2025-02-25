#include "quihelper.h"

#define SPDLOG_ACTIVE_LEVEL SPDLOG_LEVEL_TRACE
#include <spdlog/spdlog.h>

#include <QtNetwork>
#include <QMessageBox>
#include <QPushButton>

#include <filesystem>
#include <fstream>
#include <exception>

#include <nlohmann/json.hpp>

using namespace std::string_literals;

std::string QUIHelper::m_username {""s};
std::string QUIHelper::m_passwd {""s};

ClientConfig QUIHelper::m_client_config;

QUIHelper::QUIHelper()
{

}

void QUIHelper::init()
{
    SPDLOG_INFO("QUIHelper init");

    bool useSystemNetworkProxy {false};
    SPDLOG_INFO("QNetworkProxyFactory: set use system configuration:{0}",useSystemNetworkProxy);
    QNetworkProxyFactory::setUseSystemConfiguration(useSystemNetworkProxy);

    initClientConfig();
}

int QUIHelper::showMessageBox(const QString &info, MsgBoxType msgBoxType)
{
    int result = 0;

    switch (msgBoxType) {
        case MsgBoxType::INFO_TYPE:
            showMessageBoxInfo(info);
            break;
        case MsgBoxType::WARN_TYPE:
            showMessageBoxWarn(info);
            break;
        case MsgBoxType::ERROR_TYPE:
            showMessageBoxError(info);
            break;
        case MsgBoxType::QUESTION_TYPE:
            result = showMessageBoxQuestion(info);
            break;
        default:
            break;
    }

    return result;
}

void QUIHelper::showMessageBoxInfo(const QString &info)
{
    QMessageBox box;
    box.setWindowTitle(QString("提示"));
    box.setIcon(QMessageBox::Information);
    box.setText(info);

    QPushButton* yesBtn = box.addButton(QString("确定"), QMessageBox::ActionRole);
    box.exec();

    if(box.clickedButton() == yesBtn)
    {
        SPDLOG_INFO("customButton clicked");
    }
}

void QUIHelper::showMessageBoxWarn(const QString &info)
{
    QMessageBox box;
    box.setWindowTitle(QString("警告"));
    box.setIcon(QMessageBox::Warning);
    box.setText(info);


    QPushButton* yesBtn = box.addButton(QString("确定"), QMessageBox::ActionRole);
    box.exec();

    if(box.clickedButton() == yesBtn)
    {
        SPDLOG_INFO("yesBtn clicked");
    }
}

void QUIHelper::showMessageBoxError(const QString &info)
{
    QMessageBox box;
    box.setWindowTitle(QString("错误"));
    box.setIcon(QMessageBox::Critical);
    box.setText(info);


    QPushButton* yesBtn = box.addButton(QString("确定"), QMessageBox::ActionRole);
    box.exec();

    if(box.clickedButton() == yesBtn)
    {
        SPDLOG_INFO("yesBtn clicked");
    }
}

int QUIHelper::showMessageBoxQuestion(const QString &info)
{
    QMessageBox box;
    box.setWindowTitle(QString("询问"));
    box.setIcon(QMessageBox::Question);
    box.setText(info);

    QPushButton* yesBtn = box.addButton(QString("确定"), QMessageBox::ActionRole);
    QPushButton* cancellBtn = box.addButton(QString("取消"), QMessageBox::ActionRole);
    box.exec();

    int ret = static_cast<int>(QMessageBox::StandardButton::Cancel);
    if(box.clickedButton() == yesBtn)
    {
        SPDLOG_INFO("yesBtn clicked");
        ret = static_cast<int>(QMessageBox::StandardButton::Yes);;
    }
    else if(box.clickedButton() == cancellBtn)
    {
        SPDLOG_INFO("cancellBtn clicked");
        ret = static_cast<int>(QMessageBox::StandardButton::Cancel);;
    }
    return ret;
}

void QUIHelper:: initClientConfig()
{
    auto filePath = std::filesystem::current_path().append("config.ini");
    SPDLOG_INFO("filePath:{0}",filePath.string());

    std::string file_contents;
    std::string filename = filePath.string();
    std::ifstream ifs(filename);
    if(ifs.is_open())
    {
        file_contents.assign(std::istreambuf_iterator<char>(ifs), std::istreambuf_iterator<char>());
        ifs.close();
        SPDLOG_INFO("read content from {0}:{1}",filename,file_contents);
    }
    else
    {
        SPDLOG_INFO("not find config file:{0}",filename);
        return;
    }

    try
    {
        auto j_content = nlohmann::json::parse(file_contents);
        if(j_content.contains("serverIp") && j_content["serverIp"].is_string() && j_content.contains("serverPort") && j_content["serverPort"].is_number())
        {
            m_client_config.ip = j_content["serverIp"].get<std::string>();
            m_client_config.port = j_content["serverPort"];
        }
    }
    catch (std::exception& e)
    {
        SPDLOG_INFO("find exception in parse client config file.what:{0}",e.what());
    }
}
