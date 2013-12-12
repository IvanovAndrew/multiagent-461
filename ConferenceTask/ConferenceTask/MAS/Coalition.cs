using System;
using System.Collections.Generic;

namespace ConferenceTask.MAS
{
    class Coalition : ICommunication
    {
        public List<Agent> Members { get; set; }

        private const float Quorum = (float)0.5;
        public Schedule Schedule { get; set; }
    
        private Message _answer;

        public Coalition()
        {
            Members = new List<Agent>();
        }
        
        /// <summary>
        /// Voting among the agents from population.
        /// If not less than half of the approved agents, it returns true
        /// </summary>
        /// <param name="newSchedule">new version of schedule</param>
        /// <returns></returns>
        public bool Voting(Schedule newSchedule)
        {
            var count = 0;
            foreach (var agent in Members)
            {
                if (agent.IsGoodShedule(newSchedule))
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
                //if received message from new agent then send current Schedule
                case MessageType.Type.NEWAGENT:
                    _answer.Type = MessageType.Type.CURRENTSHEDULE;
                    _answer.Schedule = Schedule;
                    break;

                // if received the modificated Schedule then do voting. 
                case MessageType.Type.MODIFIEDSHEDULE:
                    if (Voting(msg.Schedule))
                    {
                        Schedule = msg.Schedule;
                        _answer.Type = MessageType.Type.ACCEPTAGENT;
                    }
                    else
                    {
                        _answer.Type = MessageType.Type.REJECTAGENT;
                    }
                    break;

                default:
                    throw new Exception("Incorrect message to coalition!");
//                    break;
            }
        }
        #endregion
    }
}