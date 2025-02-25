#include "client.h"
#include "ui_client.h"
#include "qevent.h"

#include "quihelper.h"
#include "common_proto.h"

#define SPDLOG_ACTIVE_LEVEL SPDLOG_LEVEL_TRACE
#include <spdlog/spdlog.h>

#include <QDate>
#include <QTime>
#include <QFontDialog>
#include <QColorDialog>
#include <QFileDialog>
#include <QFile>

#include <string>
using namespace std::string_literals;
using namespace std::chrono_literals;

#define G_DATES qPrintable(QDate::currentDate().toString("yyyy-MM-dd"))
#define G_TIMEMS qPrintable(QTime::currentTime().toString("HH:mm:ss.zzz"))

Client::Client(QWidget *parent) :
    QWidget(parent),
    ui(new Ui::Client)
{
    SPDLOG_INFO("init Client");
    ui->setupUi(this);

    this->initForm();
    this->initConfig();
}

Client::~Client()
{
    SPDLOG_INFO("release Client");
    delete ui;
}

void Client::initForm()
{
    SPDLOG_INFO("Client init form");
    QFont font;
    font.setPixelSize(12);
    ui->txtMain->setFont(font);
    // 取消聊天框焦点，防止编辑
    ui->txtMain->setFocusPolicy(Qt::FocusPolicy::NoFocus);

    m_is_ok.store(false);

    m_socket = new QTcpSocket(this);
    connect(m_socket, SIGNAL(connected()), this, SLOT(connected()));
    connect(m_socket, SIGNAL(disconnected()), this, SLOT(disconnected()));
    connect(m_socket, SIGNAL(readyRead()), this, SLOT(readData()));
    connect(m_socket, SIGNAL(errorOccurred(QAbstractSocket::SocketError)), this, SLOT(error()));

    connect(ui->btnSave,&QPushButton::clicked,this,&Client::btnSaveClicked);
    connect(ui->btnClear,&QPushButton::clicked,this,&Client::btnClearClicked);
    connect(ui->btnSend,&QPushButton::clicked,this,&Client::btnSendClicked);

    ui->sendTextEdit->installEventFilter(this);

    //字号
    connect(ui->fontSizeBtn,&QPushButton::clicked,this,[this]()
            {
                bool ok = true;
                auto f = QFontDialog::getFont(&ok);
                if(ok)
                {
                    this->ui->txtMain->setFont(f);
                }
            });
    connect(ui->sendFileBtn,&QPushButton::clicked,this,&Client::btnSendFileClicked);
}

void Client::initConfig()
{
    SPDLOG_INFO("Client init config");

    m_connect_timer = new QTimer(this);
    connect(m_connect_timer, &QTimer::timeout, this,&Client::btnConnectClicked);
    m_connect_timer->setInterval(3000);
    m_connect_timer->start();
}

void Client::append(int type, const QString &data, bool clear,const std::string& sender)
{
    static int currentCount = 0;
    static int maxCount = 65535;

    if (clear)
    {
        SPDLOG_INFO("clear dialog");
        ui->txtMain->clear();
        currentCount = 0;
        return;
    }

    if (currentCount >= maxCount)
    {
        SPDLOG_INFO("clear dialog");
        ui->txtMain->clear();
        currentCount = 0;
    }

    //不同类型不同颜色显示
    QString strType;
    if (type == 0)
    {
        strType = QString::fromStdString(QUIHelper::m_username);
        ui->txtMain->setTextColor(QColor(0x22,0xA3,0xA9));
    }
    else if (type == 1)
    {
        sender.empty() == true ? strType = "接收" : strType = QString::fromStdString(sender);
        ui->txtMain->setTextColor(QColor(0x75,0x37,0x75));
    }
    else
    {
        strType = "系统";
        ui->txtMain->setTextColor(QColor(0xD6,0x4D,0x54));
    }

    QString strData = data;
    strData = QString("[%1 %2]  %3：%4").arg(G_DATES,G_TIMEMS,strType,strData);
    ui->txtMain->append(strData);
    currentCount++;
}

void Client::connected()
{
    m_is_ok.store(true);

    auto localAddrInfo = QString("本地地址:") + m_socket->localAddress().toString() + QString(" 本地端口:") + QString::number(m_socket->localPort());
    auto peerAddrInfo = QString("远程地址:") + m_socket->peerAddress().toString() + QString(" 远程端口:") + QString::number(m_socket->peerPort());
    SPDLOG_INFO("connected ok.localAddrInfo:{0},peerAddrInfo:{1}",localAddrInfo.toStdString(),peerAddrInfo.toStdString());

    nlohmann::json obj = json_base_obj;
    obj["timestamp"] = std::to_string(G_TIMESTAMP);
    obj["content"] = QUIHelper::m_username;
    this->sendData(QString::fromStdString(obj.dump()),false);
}

void Client::disconnected()
{
    SPDLOG_INFO("Client disconnected");
    m_is_ok.store(false);
}

void Client::error()
{
    static std::string lastMsg{""s};

    auto curMsg = m_socket->errorString();
    if(lastMsg != curMsg.toStdString())
    {
        //append(2, curMsg);
        SPDLOG_INFO("socket error:{0}",curMsg.toStdString());
        lastMsg = curMsg.toStdString();
    }
}

void Client::readData()
{
    SPDLOG_INFO("Client read data");
    QByteArray data = m_socket->readAll();
    if (data.length() <= 0) {
        return;
    }
    SPDLOG_INFO("recv msg.len:{0},msg:{1}",data.length(),data.toStdString());

    try
    {
        auto j_obj = nlohmann::json::parse(data.toStdString());
        if(j_obj["type"] == "file")
        {
            static std::string static_file_path = "";
            std::string msgType = j_obj["msg_type"];
            if(msgType == "metadata")
            {
                std::string content = j_obj["content"];
                std::string filePath = content;
                QFile file(QString::fromStdString(filePath));
                file.open(QIODevice::WriteOnly);
                static_file_path = filePath;
                SPDLOG_INFO("cur static_file_path:{0}, fileName:{1}",static_file_path, file.fileName().toStdString());
                file.close();
            }
            else if (msgType == "data")
            {
                auto content = j_obj["content"];
                auto dataBase64 = QByteArray::fromStdString(content);
                auto data = QByteArray::fromBase64(dataBase64);
                QFile file(QString::fromStdString(static_file_path));
                file.open(QIODevice::WriteOnly);
                file.write(data);
                file.close();
                QUIHelper::showMessageBox("接收到文件",MsgBoxType::INFO_TYPE);
            }
            else
            {
                SPDLOG_WARN("not support msg_type:{0}",msgType);
            }
        }
        else if(j_obj["type"] == "text")
        {
            SPDLOG_INFO("recv text.msg:{0}",data.toStdString());
            if(j_obj.contains("sender"))
            {
                if(j_obj["sender"] == "server"s)
                {
                    SPDLOG_WARN("recv msg from serser");
                }
                else
                {
                    std::string content = j_obj["content"];
                    auto dataBase64 = QByteArray::fromStdString(content);
                    auto data = QByteArray::fromBase64(dataBase64);
                    std::string dataStr = data.toStdString();

                    SPDLOG_INFO("append msg to windows.msg:{0}",dataStr);
                    append(1, QString::fromStdString(dataStr), false,j_obj["sender"]);
                }
            }
        }
        else
        {
            SPDLOG_WARN("unsupported type");
        }
    }
    catch (const std::exception& e)
    {
        SPDLOG_WARN("find exception.e:{0}", e.what());
    }
    catch (...)
    {
        SPDLOG_ERROR("find unkonw exception");
    }
}

void Client::sendData(const QString &data,bool isAppend)
{
    SPDLOG_INFO("Client send data");
    QByteArray buffer;
    buffer = data.toUtf8();
    m_socket->write(buffer);
    SPDLOG_INFO("send {0} to server",data.toStdString());
    if(isAppend)
    {
        append(0, data);
    }
}

void Client::btnConnectClicked()
{
    if(!m_is_ok.load())
        m_socket->connectToHost(QUIHelper::m_client_config.ip.c_str(), QUIHelper::m_client_config.port);
}

void Client::btnSaveClicked()
{
    SPDLOG_INFO("Client button save clicked");
    QString data = ui->txtMain->toPlainText();
    if(!data.isEmpty())
    {
        SPDLOG_INFO("save data:{0}",data.toStdString());
        btnClearClicked();
        QUIHelper::showMessageBox("已保存至日志文件中",MsgBoxType::INFO_TYPE);
    }
    else
    {
        QUIHelper::showMessageBox("没有找到需要保存的信息",MsgBoxType::WARN_TYPE);
    }
}

void Client::btnClearClicked()
{
    SPDLOG_INFO("Client button clear clicked");
    append(0,"",true);
}

void Client::btnSendClicked()
{
    SPDLOG_INFO("Client button Send clicked");
    if(!m_is_ok.load())
    {
        (void)QUIHelper::showMessageBox(tr("未连接"),MsgBoxType::WARN_TYPE);
        return;
    }

    auto data = ui->sendTextEdit->toPlainText();
    if(data.length() <= 0)
        return;

    QByteArray dataByteArr = data.toUtf8();
    auto dataBase64 = dataByteArr.toBase64();

    nlohmann::json obj = json_base_obj;
    obj["sender"] = QUIHelper::m_username;
    obj["timestamp"] = std::to_string(G_TIMESTAMP);
    obj["content"] = dataBase64.toStdString();
    sendData(QString::fromStdString(obj.dump()),false);
    append(0,data);
    ui->sendTextEdit->clear();
}

void Client::btnSendFileClicked()
{
    SPDLOG_INFO("btnSendFileClicked");
    if(!m_is_ok.load())
    {
        (void)QUIHelper::showMessageBox(tr("未连接"),MsgBoxType::WARN_TYPE);
        return;
    }

    auto filePath = QFileDialog::getOpenFileName(this,"选择文件");
    if(filePath.isEmpty())
        return;

    SPDLOG_INFO("start transfer file:{0}",filePath.toStdString());
    QFile file(filePath);
    if(!file.open(QIODevice::ReadOnly))
    {
        QUIHelper::showMessageBox("打开文件失败，请重新尝试",MsgBoxType::WARN_TYPE);
        return;
    }

    if(file.size() > 1024 * 32)
    {
        QUIHelper::showMessageBox("文件过大，请上传小于 32KB 大小的文件",MsgBoxType::WARN_TYPE);
        return;
    }

    // 发送文件名
    QFileInfo fileInfo(filePath);
    auto fileName = fileInfo.fileName();

    nlohmann::json obj = json_base_obj;
    obj["type"] = "file";
    obj["sender"] = QUIHelper::m_username;
    obj["timestamp"] = std::to_string(G_TIMESTAMP);
    obj["msg_type"] = "metadata";
    obj["content"] = fileName.toStdString();
    this->sendData(QString::fromStdString(obj.dump()),false);
    // 降低粘包的概率
    std::this_thread::sleep_for(500ms);

    auto fileData = file.readAll();
    auto fileDataBase64 = fileData.toBase64();
    obj["timestamp"] = std::to_string(G_TIMESTAMP);
    obj["msg_type"] = "data";
    obj["content"] = fileDataBase64.toStdString();
    this->sendData(QString::fromStdString(obj.dump()),false);
    file.close();
}

void Client::keyPressEvent(QKeyEvent *event)
{
    if (event->key() == Qt::Key_Enter || event->key() == Qt::Key_Return)
    {
        this->btnSendClicked();
    }
    else
    {
        QWidget::keyPressEvent(event);
    }
}

bool Client::eventFilter(QObject *obj, QEvent *event)
{
    if (obj->isWidgetType() && event->type() == QEvent::KeyPress)
    {
        QKeyEvent *keyEvent = static_cast<QKeyEvent *>(event);
        if (keyEvent->key() == Qt::Key_Return || (keyEvent->key() == Qt::Key_Enter && keyEvent->modifiers() == Qt::AltModifier))
        {
            if(keyEvent->modifiers() == Qt::AltModifier)
            {
                ui->sendTextEdit->insertPlainText("\n");
                return true;
            }
            else
            {
                btnSendClicked();
                return true;
            }
        }
    }

    return false;
}

bool Client::getIsOk()
{
    return this->m_is_ok.load();
}
