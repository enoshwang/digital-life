#pragma once

#include <QLabel>
#include <QPushButton>
#include <QTextEdit>
#include <QWidget>

const size_t QABOUT_WIDGET_WIDTH{320};
const size_t QABOUT_WIDGET_HEIGHT{240};

class QAbout final : public QWidget {
    Q_OBJECT
public:
    explicit QAbout(QWidget* parent = nullptr);
    ~QAbout() override;

public slots:
    void onSlotExitBtn() noexcept;

private:
    void initUiComponent() noexcept;

private:
    QPushButton* exitBtn{};
    QLabel*      infoLabel{};
    QLabel*      titleLabel{};
    QLabel*      authorLabel{};
    QTextEdit*   infoTextEdit{};
};
