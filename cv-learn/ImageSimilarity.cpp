#include "ImageSimilarity.hpp"

#include <iostream>


ImageSimilarity::ImageSimilarity() : detector(cv::ORB::create()), matcher(cv::BFMatcher(cv::NORM_HAMMING)){}

double ImageSimilarity::calculateSimilarity(const cv::Mat& img1, const cv::Mat& img2, const std::string matchesFilename) const
{
    if (img1.empty() || img2.empty())
    {
        std::cerr << "Image is empty!" << std::endl;
        return 0.0;
    }

    // 检测 ORB 特征点和描述符
    std::vector<cv::KeyPoint> keypoints1, keypoints2;
    cv::Mat descriptors1, descriptors2;

    detector->detectAndCompute(img1, cv::noArray(), keypoints1, descriptors1);
    detector->detectAndCompute(img2, cv::noArray(), keypoints2, descriptors2);

    // 检查描述符是否为空
    if (descriptors1.empty() || descriptors2.empty()) {
        std::cerr << "特征点检测失败！" << std::endl;
        return 0.0;
    }

    // 使用 BFMatcher 进行匹配
    std::vector<cv::DMatch> matches;
    matcher.match(descriptors1, descriptors2, matches);

    // 找到最小和最大距离
    double max_dist = 0, min_dist = 100;
    for (int i = 0; i < descriptors1.rows; i++)
    {
        double dist = matches[i].distance;
        if (dist < min_dist) min_dist = dist;
        if (dist > max_dist) max_dist = dist;
    }

    // 过滤匹配结果，只保留好的匹配点
    std::vector<cv::DMatch> good_matches;
    for (int i = 0; i < descriptors1.rows; i++) 
    {
        if (matches[i].distance <= std::max(2 * min_dist, 30.0)) 
        {
            good_matches.push_back(matches[i]);
        }
    }

    // 计算相似度
    double similarity = (double)good_matches.size() / (double)std::min(keypoints1.size(), keypoints2.size()) * 100.0;

    // 绘制并保存匹配结果
    if (!matchesFilename.empty())
    {
        cv::Mat img_matches;
        drawMatches(img1, keypoints1, img2, keypoints2, good_matches, img_matches, cv::Scalar::all(-1), cv::Scalar::all(-1), std::vector<char>(), cv::DrawMatchesFlags::NOT_DRAW_SINGLE_POINTS);
        cv::imwrite(matchesFilename,img_matches);
    }
    
    // 返回相似度
    return similarity;
}