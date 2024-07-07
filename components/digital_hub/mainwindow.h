#pragma once

#include <QMainWindow>
#include <QCloseEvent>
#include <QStackedWidget>

#include "client.h"
#include "player.h"

#include <vector>
#include <string>

QT_BEGIN_NAMESPACE
namespace Ui { class MainWindow; }
QT_END_NAMESPACE

class Client;
class MainWindow final: public QMainWindow
{
    Q_OBJECT

public:
    MainWindow(QWidget *parent = nullptr);
    ~MainWindow();

private slots:
    // 媒体
    void slotsActionPlayerTrigger();
    void slotsActionImageViewerTrigger();
    // 通讯
    void slotsActionOnlineRoomTrigger();
    // 慧文
    void slotsActionHomePageTrigger();
    // 数据分析
    void slotActionBroswerDataTrigger();

    // 窗口切换
    void toggle_windows(QWidget*);

private:
    void initMenu();

private:
    // 通讯
    std::unique_ptr<Client> client;

    // 多媒体
    std::unique_ptr<Player> player;

    // main
    Ui::MainWindow *ui;    
    QTimer* m_timer;

    QStackedWidget* m_stackedwidget;
    std::vector<std::pair<std::string,uint64_t>> m_widgetIndex;
};
