#include "register.h"
#include "ui_register.h"

#define SPDLOG_ACTIVE_LEVEL SPDLOG_LEVEL_TRACE
#include <spdlog/spdlog.h>

Register::Register(QWidget *parent) :
    QDialog(parent),
    ui(new Ui::Register)
{
    ui->setupUi(this);
    this->setWindowTitle(tr("注册"));
}

Register::~Register()
{
    delete ui;
    SPDLOG_INFO("release register");
}
