#include "onlineChatRoom.hpp"

#define SPDLOG_ACTIVE_LEVEL SPDLOG_LEVEL_TRACE
#include <spdlog/spdlog.h>

#include <nlohmann/json.hpp>
#include <unordered_map>

#include "common_proto.hpp"
#include "reactor.hpp"

#include <my_utils/utils.hpp>

extern ew::my_utils::Queue<OnlineChatRoomMessage> g_ocr_msg_queue;

std::unordered_map<int, OCRClientInfo> g_ocr_client_info_map;

OnlineChatRoom::OnlineChatRoom(/* args */)
{
    SPDLOG_INFO("OnlineChatRoom construct");
}

OnlineChatRoom::~OnlineChatRoom()
{
    SPDLOG_INFO("OnlineChatRoom destruct");
}

void OnlineChatRoom::run(Reactor* reactor) noexcept
{
    SPDLOG_INFO("OnlineChatRoom run");

    while (true)
    {
        OnlineChatRoomMessage msg;
        g_ocr_msg_queue.wait_and_pop(msg);
        SPDLOG_INFO("OnlineChatRoom get msg from queue.fd:{0},addr:{1},msg size:{2}", msg.m_fd, msg.m_addr,
                    msg.m_message.size());

        auto brackets = find_range_brackets(msg.m_message);
        for (auto const& i : brackets)
        {
            SPDLOG_INFO("OnlineChatRoom get {0} msg.brackets.first:{1},brackets.second:{2}", brackets.size(), i.first,
                        i.second);
            std::vector<char> msg_vec(msg.m_message.begin() + i.first, msg.m_message.begin() + i.second + 1);
            OnlineChatRoomMessage msg_tmp;
            msg_tmp.m_fd      = msg.m_fd;
            msg_tmp.m_addr    = msg.m_addr;
            msg_tmp.m_message = msg_vec;

            try
            {
                nlohmann::json j = nlohmann::json::parse(msg_vec);
                if (j["type"] == "text")
                {
                    handleTextMsg(msg_tmp, reactor);
                }
                else if (j["type"] == "file")
                {
                    handleFileMsg(msg_tmp, reactor);
                }
                else
                {
                    SPDLOG_WARN("OnlineChatRoom unknown type");
                }
            } catch (const std::exception& e)
            {
                SPDLOG_WARN("find exception.e:{0}", e.what());
            }
        }
    }
}

void OnlineChatRoom::handleTextMsg(const OnlineChatRoomMessage& msg, Reactor* reactor) noexcept
{
    SPDLOG_INFO("OnlineChatRoom:text type. get msg from queue.fd:{0},addr:{1},msg size:{2}", msg.m_fd, msg.m_addr,
                msg.m_message.size());

    try
    {
        nlohmann::json j = nlohmann::json::parse(msg.m_message);
        SPDLOG_INFO("start deal text msg:{0}", j.dump());
        if (j["sender"] == "client")
        {
            OCRClientInfo clientInfo;
            clientInfo.m_username           = j["content"].get<std::string>();
            g_ocr_client_info_map[msg.m_fd] = clientInfo;
            SPDLOG_INFO("OnlineChatRoom: set client info. fd:{0},username:{1}", msg.m_fd, clientInfo.m_username);
        }
        else
        {
            nlohmann::json sendJson = json_base_obj;
            sendJson["sender"]      = g_ocr_client_info_map[msg.m_fd].m_username;
            sendJson["timestamp"]   = G_TIMESTAMP;
            sendJson["content"]     = j["content"];
            auto sendMsg            = sendJson.dump();

            auto listendFd = reactor->m_evDemux->getListenFd();
            for (auto& it : reactor->m_ep_events)
            {
                //if (it.first != msg.m_fd && it.first != listendFd)
                if(it.first != listendFd)
                {
                    SPDLOG_INFO("OnlineChatRoom send msg:{0} to fd: {1},len:{2}", sendMsg, it.first, sendMsg.size());
                    it.second->handWrite(reactor, std::vector<char>(sendMsg.begin(), sendMsg.end()));
                }
            }
        }
    } catch (const std::exception& e)
    {
        SPDLOG_WARN("find exception.e:{0}", e.what());
    }
}

void OnlineChatRoom::handleFileMsg(const OnlineChatRoomMessage& msg, Reactor* reactor) noexcept
{
    SPDLOG_INFO("OnlineChatRoom: file type,and msg:{0}", std::string(msg.m_message.begin(), msg.m_message.end()));
    try
    {
        nlohmann::json j    = nlohmann::json::parse(msg.m_message);
        std::string msyType = j["msg_type"];
        if (msyType == "metadata")
        {
            SPDLOG_INFO("OnlineChatRoom: get file metadata:{0} from queue.fd:{1},addr:{2},msg size:{3}", msyType,
                        msg.m_fd, msg.m_addr, msg.m_message.size());
        }
        else if (msyType == "data")
        {
            SPDLOG_INFO("OnlineChatRoom: get file data:{0} from queue.fd:{1},addr:{2},msg size:{3}", msyType, msg.m_fd,
                        msg.m_addr, msg.m_message.size());
        }
        else
        {
            SPDLOG_WARN("OnlineChatRoom: unknown file msg type:{0}", msyType);
        }

        j["sender"]      = g_ocr_client_info_map[msg.m_fd].m_username;
        auto sendMsg            = j.dump();

        auto listendFd = reactor->m_evDemux->getListenFd();
        for (auto& it : reactor->m_ep_events)
        {
            //if (it.first != msg.m_fd && it.first != listendFd)
            if (it.first != listendFd)
            {
                SPDLOG_INFO("OnlineChatRoom send msg to fd: {0},len:{1}", it.first, sendMsg.size());
                it.second->handWrite(reactor, std::vector<char>(sendMsg.begin(), sendMsg.end()));
            }
        }
    } catch (const std::exception& e)
    {
        SPDLOG_WARN("find exception.e:{0}", e.what());
    }
}

std::vector<std::pair<size_t, size_t>> OnlineChatRoom::find_range_brackets(const std::vector<char>& msg) const noexcept
{
    std::vector<std::pair<size_t, size_t>> ret;

    for (size_t i = 0; i < msg.size(); ++i)
    {
        if (msg[i] == '{')
        {
            for (size_t j = i + 1; j < msg.size(); ++j)
            {
                if (msg[j] == '}')
                {
                    ret.push_back(std::make_pair(i, j));
                    i = j;
                    break;
                }
            }
        }
    }

    return ret;
}
