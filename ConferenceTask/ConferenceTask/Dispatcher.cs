using System;
using System.Collections.Generic;

namespace ConferenceTask
{
    public class Dispatcher
    {
        /// <summary>
        /// row - reports
        /// column - listeners
        /// </summary>
        private readonly int[,] _matrix;

        private readonly Dictionary<int, Agent> _agents = new Dictionary<int, Agent>();
        
        private List<Agent> _coalition = new List<Agent>();
        private List<Agent> _opposition = new List<Agent>();
        /// <summary>
        /// row - times
        /// column - flow
        /// </summary>
        public int[,] _shedule;

        private readonly int Listeners;
        private readonly int Reports;
        private readonly int Times;
        public const int Flows = Generator.Flows;
        private const float Quorum = (float) 0.5;

        public Dispatcher(int[,] matrix)
        {
            _matrix = matrix;

            Reports = _matrix.GetLength(0);
            Listeners = _matrix.GetLength(1);
            
            Times = Reports/Flows;

            _shedule = new int[Times, Flows];
            InitialFillShedule();

            CreatesAgents();
        }

        private void InitialFillShedule()
        {
            int count = 0;
            for (int i = 0; i < Times; i++)
            {
                for (int j = 0; j < Flows; j++)
                {
                    _shedule[i, j] = count++;
                }
            }
        }

        private void CreatesAgents()
        {
            for (int i = 0; i < Listeners; i++)
            {
                var ratings = new int[Reports];
                for (int j = 0; j < Reports; j++)
                {
                    ratings[j] = _matrix[j, i];
                }
                var agent = new Agent(i, ratings, Times);
                _agents.Add(i, agent);
            }
        }

        public void CreateShedule()
        {
            foreach (KeyValuePair<int, Agent> pair in _agents)
            {
                var agent = pair.Value;
                agent.AnaliseShedule(_shedule);

                if (_coalition.Count == 0)
                {
                    if (agent.IsGoodShedule())
                        continue;
                    _shedule = agent.GetShedule(_shedule);
                    _coalition.Add(agent);
                }

                else
                {
                    if (agent.IsGoodShedule())
                    {
                        _coalition.Add(agent);
                    }
                    else
                    {
                        var newShedule = agent.GetShedule(_shedule);
                        if (Voting(newShedule))
                        {
                            _shedule = newShedule;
                            _coalition.Add(agent);
                        }
                        else
                        {
                            _opposition.Add(agent);
                        }
                    }
                }
            }
        }

        private bool Voting(int [,] newShedule)
        {
            int count = 0;
            foreach (Agent agent in _coalition)
            {
                agent.AnaliseShedule(newShedule);
                if (agent.IsGoodShedule())
                {
                    count++;
                }
            }

            return count >= _coalition.Count*Quorum;
        }

        

        
        //public void CreateFirstCoalition()
        //{
        //    for(int i = 2; i < )
        //}

        ///// <summary>
        ///// Creates first version of shedule
        ///// </summary>
        ///// <param name="first">First listener</param>
        ///// <param name="second">Second listener</param>
        //private void CreateFirstShedule(int first, int second)
        //{
        //    var topReportsForOne = GetTopReports(first);
        //    var topRepostsForTwo = GetTopReports(second);

        //    for (int i = 0; i < Times; i++)
        //    {
        //        _shedule[0, i] = topReportsForOne[i];
        //    }

        //    var usedReports = new List<int>(topReportsForOne);
        //    int count = 0;
        //    foreach (int report in topRepostsForTwo)
        //    {
        //        if (topReportsForOne.Contains(report))
        //            continue;
                
        //        usedReports.Add(report);

        //        while (_shedule[0, count] == report)
        //        {
        //            count++;
        //        }
        //        _shedule[1, count] = report;
        //    }

        //    // fill by unused reports
        //    for (int report = 0; report < Reports; report++)
        //    {
        //        if (usedReports.Contains(report)) continue;
        //        
        //        for (int time = 0; time < Times; time++)
        //        {
        //            for (int flow = 1; flow < Flows; flow++)
        //            {
        //                if (_shedule[time, flow] < 0)
        //                {
        //                    _shedule[time, flow] = report;
        //                    break;
        //                }
        //            }
        //        }
        //    }
            
        //}

        ///// <summary>
        ///// Returns top $Times$ reports for the listener
        ///// </summary>
        ///// <param name="listener">Concrete listener</param>
        ///// <returns>List of top $Times$ reports for the listener</returns>
        //private List<int> GetTopReports(int listener)
        //{
        //    // first element is number of report
        //    // second element is weight of report
        //    // sorted by descending
        //    var topReports = new List <KeyValuePair<int, int>>();
        //    int lastElem = Times - 1;

        //    for (int i = 0; i < Reports; i++)
        //    {
        //        if (i < Times)
        //        {
        //            topReports.Add(new KeyValuePair<int, int>(i, _matrix[listener, i]));
        //            topReports = topReports.OrderByDescending(pair => pair.Value).ToList();
        //        }
        //        else
        //        {
        //            if (_matrix[listener, i] > topReports[lastElem].Value)
        //            {
        //                topReports.RemoveAt(lastElem);
        //                topReports.Add(new KeyValuePair<int, int>(i, _matrix[listener, i]));
        //                topReports = topReports.OrderByDescending(pair => pair.Value).ToList();
        //            }
        //        }
        //    }

        //    var result = new List<int>();
        //    foreach (var pair in topReports)
        //    {
        //        result.Add(pair.Key);
        //    }
        //    return result;
        //}
    }
}
