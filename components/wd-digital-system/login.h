#ifndef LOGIN_H
#define LOGIN_H

#include <QDialog>

#include "register.h"

namespace Ui {
class Login;
}

class Login : public QDialog
{
    Q_OBJECT

public:
    explicit Login(QWidget *parent = nullptr);
    ~Login();

private slots:
    void doLogin();
    void doRegister();

private:
    bool doUsernameVerification(const QString& userName);

private:
    Ui::Login *ui;
    Register *registerDialog;
};

#endif // LOGIN_H
