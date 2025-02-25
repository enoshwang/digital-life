#include "aichat.h"

#include "ui_aichat.h"

AIChat::AIChat(QWidget *parent) : QWidget(parent), ui(new Ui::AIChat)
{
    ui->setupUi(this);
}

AIChat::~AIChat()
{
    delete ui;
}
