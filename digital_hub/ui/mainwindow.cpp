#include "mainwindow.h"
#include "./ui_mainwindow.h"

#define SPDLOG_ACTIVE_LEVEL SPDLOG_LEVEL_TRACE
#include <spdlog/spdlog.h>

#include "client.h"
#include "browserdataanalysis.h"
#include "imageviewer.h"

#include <QAction>
#include <QFileDialog>
#include <QGuiApplication>
#include <QMenuBar>
#include <QScreen>
#include <QTime>

#include <exception>

using namespace std::literals;

static const size_t MAX_STACKED_WIDGET_COUNT = 8;

MainWindow::MainWindow(QWidget *parent) : QMainWindow(parent), ui(new Ui::MainWindow)
{
    ui->setupUi(this);
    this->setWindowTitle(tr("数字中心"));

    // 自适应分辨率与缩放，界面占据屏幕的一半进行展示
    auto screen = QGuiApplication::primaryScreen();
    auto screen_geometry = screen->geometry();
    auto screen_width = screen_geometry.width();
    auto screen_height = screen_geometry.height();
    SPDLOG_INFO("Primary screen width:{0},screen height:{1}",screen_width,screen_height);
    this->resize(screen_width / 2, screen_height / 2);

    // 初始化菜单栏
    initMenu();

    m_stackedwidget = new QStackedWidget(this);
    m_stackedwidget->addWidget(ui->centralwidget);

    m_widgetIndex.reserve(MAX_STACKED_WIDGET_COUNT);
    m_widgetIndex.emplace_back("centralwidget"s,m_widgetIndex.size());
    for(auto& i:m_widgetIndex)
    {
        SPDLOG_INFO("key:{0},value:{1}",i.first,i.second);
    }

    this->setCentralWidget(m_stackedwidget);

    for(const auto& i: m_widgetIndex)
    {
        if(i.first == "centralwidget"s)
        {
            m_stackedwidget->setCurrentIndex(i.second);
            SPDLOG_INFO("set current index:{0},widget name:{1}",i.second,i.first);
        }
    }

    // 显示当前连接状态
    m_timer = new QTimer(this);
    connect(m_timer, &QTimer::timeout, this,[&]() {
        if(client)
        {
            bool is_in_client = false;
            auto index = m_stackedwidget->currentIndex();
            for(auto const& [x,y] : m_widgetIndex)
            {
                if(index == y)
                {
                    if(x == "client"s)
                        is_in_client = true;
                    break;
                }
            }
            if(!is_in_client)
                return;

            auto isOk = client->getIsOk();
            if(isOk)
            {
                this->statusBar()->setStyleSheet("color: green;");
                this->statusBar()->showMessage("已连接到服务器");
                SPDLOG_INFO("成功连接到服务器");
            }
            else
            {
                this->statusBar()->setStyleSheet("color: red;");
                this->statusBar()->showMessage("连接不到服务器，请检查网络，或联系管理管");
                SPDLOG_INFO("未连接到服务器");
            }
        }
    });
    m_timer->setInterval(3000);
    m_timer->start();
}

MainWindow::~MainWindow()
{
    m_widgetIndex.clear();

    delete ui;
}

void MainWindow::slotsActionPlayerTrigger()
{
    SPDLOG_INFO("Slots action Player trigger");
    try
    {
        if(!this->player)
        {
            SPDLOG_INFO("New player");
            player = std::make_unique<Player>(this);
        }

        this->toggle_windows(player.get());
    }
    catch (std::exception& e)
    {
        SPDLOG_INFO("find e:{0}",e.what());
        return;
    }
}

void MainWindow::slotsActionImageViewerTrigger()
{
    SPDLOG_INFO("show image viewer");
    auto imageViewer = new ImageViewer(this);
    imageViewer->show();
}

void MainWindow::slotsActionOnlineRoomTrigger()
{
    SPDLOG_INFO("slots action OnlineRoom trigger");
    if(!this->client)
    {
        SPDLOG_INFO("new client");
        client = std::make_unique<Client>(this);
        m_stackedwidget->addWidget(client.get());
        m_widgetIndex.emplace_back("client"s,m_widgetIndex.size());
        for(auto& i:m_widgetIndex)
        {
            SPDLOG_INFO("key:{0},value:{1}",i.first,i.second);
        }
    }
    for(const auto& i: m_widgetIndex)
    {
        if(i.first == "client"s)
        {
            m_stackedwidget->setCurrentIndex(i.second);
            SPDLOG_INFO("set current index:{0},widget name:{1}",i.second,i.first);
        }
    }
}

void MainWindow::slotsActionHomePageTrigger()
{
    SPDLOG_INFO("slots action home page trigger");
    for(const auto& i: m_widgetIndex)
    {
        if(i.first == "centralwidget"s)
        {
            m_stackedwidget->setCurrentIndex(i.second);
            SPDLOG_INFO("set current index:{0},widget name:{1}",i.second,i.first);
        }
    }
}

void MainWindow::slotActionBroswerDataTrigger()
{
    auto browserDataWidget = new BrowserDataAnalysis(this);
    browserDataWidget->show();
}

void MainWindow::toggle_windows(QWidget* window)
{
    SPDLOG_INFO("Toggle windows:{0}",window->windowTitle().toStdString());
    if(window->isVisible())
        window->hide();
    else
        window->show();
}

void MainWindow::initMenu()
{
    connect(ui->actionmain_page,&QAction::triggered,this,&MainWindow::slotsActionHomePageTrigger);

    connect(ui->actionOnlineRoom,&QAction::triggered,this,&MainWindow::slotsActionOnlineRoomTrigger);

    connect(ui->actionwd_player,&QAction::triggered,this,&MainWindow::slotsActionPlayerTrigger);
    connect(ui->actionimageViewer,&QAction::triggered,this,&MainWindow::slotsActionImageViewerTrigger);

    connect(ui->actionbrowser,&QAction::triggered,this,&MainWindow::slotActionBroswerDataTrigger);
}
