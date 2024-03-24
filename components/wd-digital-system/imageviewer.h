#ifndef IMAGEVIEWER_H
#define IMAGEVIEWER_H

#include <QMainWindow>
#include <QImage>

namespace Ui {
class ImageViewer;
}

class ImageViewer : public QMainWindow
{
    Q_OBJECT

public:
    explicit ImageViewer(QWidget *parent = nullptr);
    ~ImageViewer();

private slots:
    // 文件
    void slotsActionOpen();

private:
    void loadFile(const QString &fileName);
    void setImage(const QImage &newImage);

private:
    Ui::ImageViewer *ui;

private:
    QImage image;
    double m_scale_factor {1.0};
};

#endif // IMAGEVIEWER_H
