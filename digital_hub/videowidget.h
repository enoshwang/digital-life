#ifndef VIDEOWIDGET_H
#define VIDEOWIDGET_H

#include <QVideoWidget>
#include <QKeyEvent>
#include <QMouseEvent>
#include <QMediaPlayer>
#include <QComboBox>
#include <QLabel>
#include <QTimer>

class VideoWidget : public QVideoWidget
{
    Q_OBJECT
public:
    explicit VideoWidget(QWidget *parent = nullptr);

public slots:
    void setPlayer(QMediaPlayer *p);
    void setComBox(QComboBox *cb);

    void displaySpeed(QString);

protected:
    void keyPressEvent(QKeyEvent *event) override;
    void mouseDoubleClickEvent(QMouseEvent *event) override;
    void mousePressEvent(QMouseEvent *event) override;

private:
    QMediaPlayer *player;
    QComboBox   *comboBox;

    QLabel *speedLabel;
    QTimer *timer;
};

#endif // VIDEOWIDGET_H
