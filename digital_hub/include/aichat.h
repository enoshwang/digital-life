#ifndef AICHAT_H
#define AICHAT_H

#include <QWidget>

namespace Ui {
class AIChat;
}

class AIChat : public QWidget
{
    Q_OBJECT

public:
    explicit AIChat(QWidget *parent = nullptr);
    ~AIChat();

private:
    Ui::AIChat *ui;
};

#endif  // AICHAT_H
