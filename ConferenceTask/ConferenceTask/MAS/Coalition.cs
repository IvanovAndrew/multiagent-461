﻿using System;
using System.Collections.Generic;

namespace ConferenceTask.MAS
{
    class Coalition : ICommunication
    {
        public List<Agent> Members { get; set; }

        private const float Quorum = (float)0.5;
        private Shedule _shedule;
        private Message _answer;

        public Coalition(Shedule shedule)
        {
            Members = new List<Agent>();
            _shedule = shedule;
        }

        public Coalition()
        {
            Members = new List<Agent>();
        }

        /// <summary>
        /// voting among the agents from population.
        /// if not less than half of the approved agents, it returns true
        /// </summary>
        /// <param name="newShedule"></param>
        /// <returns></returns>
        public bool Voting(Shedule newShedule)
        {
            int count = 0;
            foreach (Agent agent in Members)
            {
                agent.AnaliseShedule(newShedule);
                if (agent.IsGoodShedule())
                {
                    count++;
                }
            }

            return count >= Members.Count * Quorum;
        }

        #region ICommuncation members
        public Message GetAnswer()
        {
            return _answer;
        }
        
        public void ReceiveMessage(Message msg)
        {
            _answer = new Message();
            switch (msg.Type)
            {
                //if received message from new agent then send current shedule
                case MessageType.Type.NEWAGENT:
                    _answer.Type = MessageType.Type.CURRENTSHEDULE;
                    _answer.Shedule = _shedule;
                    break;

                // if received the modificated shedule then do voting. 
                case MessageType.Type.MODIFIEDSHEDULE:
                    if (Voting(msg.Shedule))
                    {
                        _shedule = msg.Shedule;
                        msg.Type = MessageType.Type.ACCEPTAGENT;
                    }
                    else
                    {
                        msg.Type = MessageType.Type.REJECTAGENT;
                    }
                    break;

                default:
                    throw new Exception("Incorrect message to coalition!");
                    break;
            }
        }
        #endregion
    }
}
