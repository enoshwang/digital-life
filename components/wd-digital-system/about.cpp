#include "about.h"

#define SAFE_FREE(PTR)    \
    if (nullptr != PTR) { \
        delete PTR;       \
        PTR = nullptr;    \
    }

QAbout::~QAbout(){SAFE_FREE(exitBtn) SAFE_FREE(infoLabel) SAFE_FREE(titleLabel)
                      SAFE_FREE(authorLabel) SAFE_FREE(infoTextEdit)}

QAbout::QAbout(QWidget *parent)
    : QWidget(parent) {
    // init ui
    initUiComponent();
}
void QAbout::initUiComponent() noexcept {
    int label_w = 300;
    int label_h = 20;
    int text_w  = 300;
    int text_h  = 120;
    int btn_w   = 80;
    int btn_h   = 30;
    int btn_x   = QABOUT_WIDGET_WIDTH - btn_w;
    int btn_y   = QABOUT_WIDGET_HEIGHT - btn_h;

    titleLabel = new QLabel(this);
    titleLabel->setText(tr("ewtool"));
    titleLabel->setGeometry(20, 10, label_w, label_h);

    QFont titleFont("Microsoft YaHei", 10, QFont::Bold);
    titleLabel->setFont(titleFont);

    authorLabel = new QLabel(this);
    authorLabel->setText(tr("Author: 炎冬"));
    authorLabel->setGeometry(20, 30, label_w, label_h);

    infoLabel = new QLabel(this);
    infoLabel->setOpenExternalLinks(true);
    infoLabel->setText(
        tr("<a href=\"https://github.com/EnoshWang\">"
           "[https://github.com/EnoshWang]"));
    infoLabel->setGeometry(20, 50, label_w, label_h);

    QString info;
    info.append("                                     Statement\n");
    info.append("wdChat is a free and open source project. \n");
    info.append(
        R"(MIT License
    Copyright (c) 2020 Wendell Wang
    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.

        THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
        OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
    SOFTWARE.)");

    infoTextEdit = new QTextEdit(this);
    infoTextEdit->setText(info);
    infoTextEdit->setReadOnly(1);
    infoTextEdit->setGeometry(15, 80, text_w, text_h);

    QPalette palette;
    palette.setColor(QPalette::Text, Qt::red);
    infoTextEdit->setPalette(palette);
    QFont infoFont("Microsoft YaHei", 8, QFont::Cursive);
    infoTextEdit->setFont(infoFont);

    exitBtn = new QPushButton(this);
    exitBtn->setText(tr("OK"));
    exitBtn->setGeometry(btn_x - 80, btn_y - 5, btn_w, btn_h);
    connect(exitBtn, SIGNAL(clicked(bool)), this, SLOT(onSlotExitBtn()));
}
void QAbout::onSlotExitBtn() noexcept {
    this->close();
}
