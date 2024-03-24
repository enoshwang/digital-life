#ifndef CLIENT_H
#define CLIENT_H

#include <QWidget>
#include <QtNetwork>
#include <QNetworkProxy>
#include <QEvent>
#include <QTime>

#include <atomic>

namespace Ui {
class Client;
}

class Client : public QWidget
{
    Q_OBJECT

public:
    explicit Client(QWidget *parent = nullptr);
    ~Client();

private slots:
    void initForm();
    void initConfig();
    void append(int type, const QString &data, bool clear = false,const std::string& sender = "");

private slots:
    void connected();
    void disconnected();
    void error();
    void readData();
    void sendData(const QString &data,bool isAppend = true);

private slots:
    void btnConnectClicked();
    void btnSaveClicked();
    void btnClearClicked();
    void btnSendClicked();
    void btnSendFileClicked();

protected:
    void keyPressEvent(QKeyEvent *event) override;
    bool eventFilter(QObject *obj, QEvent *event) override;

public:
    bool getIsOk();

private:
    Ui::Client *ui;
    QTcpSocket *m_socket;
    std::atomic_bool m_is_ok{false};
    QTimer* m_connect_timer;
};

#endif // CLIENT_H
