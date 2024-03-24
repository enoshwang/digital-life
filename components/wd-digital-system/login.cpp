#include "login.h"

#include "register.h"
#include "quihelper.h"

#define SPDLOG_ACTIVE_LEVEL SPDLOG_LEVEL_TRACE
#include <spdlog/spdlog.h>

#include "ui_login.h"

Login::Login(QWidget *parent) :
    QDialog(parent),
    ui(new Ui::Login)
{
    SPDLOG_INFO("init Login");
    ui->setupUi(this);

    this->setWindowTitle(tr("登录"));

    connect(ui->loginBtn,&QPushButton::clicked,this,&Login::doLogin);
    connect(ui->registerBtn,&QPushButton::clicked,this,&Login::doRegister);
}

Login::~Login()
{
    SPDLOG_INFO("release Login");
    delete ui;
}

void Login::doLogin()
{
    SPDLOG_INFO("do login");

    auto username = ui->userEdit->text();
    auto pwd = ui->pwdEdit->text();
    SPDLOG_INFO("username:{0},passwd:{1}",username.toStdString(),pwd.toStdString());
    if(!doUsernameVerification(username))
    {
        return;
    }

    QUIHelper::m_username = username.toStdString();
    QUIHelper::m_passwd = pwd.toStdString();

    accept();
}

void Login::doRegister()
{
    SPDLOG_INFO("do register");
    registerDialog = new Register(this);
    registerDialog->show();
}

bool Login::doUsernameVerification(const QString &userName)
{
    if(userName.isEmpty())
    {
        QUIHelper::showMessageBox(tr("用户名不允许为空"),MsgBoxType::WARN_TYPE);
        return false;
    }
    return true;
}
