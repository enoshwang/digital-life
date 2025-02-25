#ifndef QUIHELPER_H
#define QUIHELPER_H

#include <QString>
#include <string>

enum MsgBoxType
{
    INFO_TYPE,
    WARN_TYPE,
    ERROR_TYPE,
    QUESTION_TYPE
};

struct ClientConfig
{
    std::string ip {"101.34.147.52"};
    unsigned short port {1504};
};

class QUIHelper
{
public:
    QUIHelper();

    // 初始化
    static void init();

    //弹出框
    static int showMessageBox(const QString &info, MsgBoxType msgBoxType = MsgBoxType::INFO_TYPE);

private:
    //弹出消息框
    static void showMessageBoxInfo(const QString &info);
    //弹出警告框
    static void showMessageBoxWarn(const QString &info);
    //弹出错误框
    static void showMessageBoxError(const QString &info);
    //弹出询问框
    static int showMessageBoxQuestion(const QString &info);

    static void initClientConfig();
public:
    static std::string m_username;
    static std::string m_passwd;

    // client
    static ClientConfig m_client_config;
};

#endif // QUIHELPER_H
