using System.Collections.Generic;
using System.Linq;

namespace ConferenceTask
{
    public class Agent
    {
        public int Id;
        private readonly int[] _ratings;
        private readonly int _times;
        private readonly int _minBound;
        
        /// <summary>
        /// Key is report
        /// Value is flow number
        /// 
        /// First dimension is time
        /// Second dimension is numberof priorities
        /// Sorted by descending
        /// </summary>
        private readonly KeyValuePair<int,int>[][] _analisedShedule;

        public Agent(int id, int[] ratings, int times)
        {
            Id = id;
            _ratings = ratings;
            _times = times;
            _analisedShedule = new KeyValuePair<int, int>[_times][];
            _minBound = CalculateBound();
        }

        private int CalculateBound()
        {
            var temp = _ratings.OrderByDescending(elem => elem).ToList();
            return temp[_times];
        }

        /// <summary>
        /// if agent likes current shedule then returns true
        /// </summary>
        /// <returns></returns>
        public bool IsGoodShedule()
        {
            for (int i = 0; i < _times; i++)
            {
                if (IsGoodTime(i)) continue;
                    return false;
            }
            return true;
        }

        /// <summary>
        /// Checks if maximal rating in the time is greather than minimal bound
        /// </summary>
        /// <param name="time"></param>
        /// <returns></returns>
        private bool IsGoodTime(int time)
        {
            var report = _analisedShedule[time][0].Key;
            return _ratings[report] >= _minBound;
        }

        public int[,] GetShedule(int[,] shedule)
        {
            int[,] newShedule;
            newShedule = shedule;
            for (int time = 0; time < _times; time++)
            {
                if (IsGoodTime(time)) continue;

                ChangeShedule(time, newShedule);
            }
            return newShedule;
        }

        private int GetSecondTopIndex()
        {
            var max = -1;
            var index = -1;
            for (int i = 0; i < _times; i++)
            {
                var report = _analisedShedule[i][1].Key;
                if (_ratings[report] > max)
                {
                    max = _ratings[report];
                    index = i;
                }
            }

            return index;
        }

        /// <summary>
        /// Changed one report from "bad" time to "good" report 
        /// </summary>
        /// <param name="badTime"></param>
        /// <param name="shedule">planning shedule</param>
        private void ChangeShedule(int badTime, int[,] shedule)
        {
            int index = GetSecondTopIndex();

            // replace two reports
            var badReport = _analisedShedule[badTime][2];
            var goodReport = _analisedShedule[index][1];

            // replace two reports in main shedule
            shedule[badTime, badReport.Value] = goodReport.Key;
            shedule[index, goodReport.Value] = badReport.Key;

            // repalce two reports in analised shedule
            _analisedShedule[badTime][2] = new KeyValuePair<int, int>(badReport.Key, goodReport.Value);
            _analisedShedule[index][1] = new KeyValuePair<int, int>(goodReport.Key, badReport.Value);
        }

        /// <summary>
        /// Creates shedule which reports are sorted by descending on each line
        /// </summary>
        /// <param name="shedule">current shedule</param>
        public void AnaliseShedule(int[,] shedule)
        {
            for (int i = 0; i < _times; i++)
            {
                _analisedShedule[i] = OrderLineByPriorityDescending(i, shedule);
            }
        }

        /// <summary>
        /// creates line of analised shedule. 
        /// sorted in descending order of priority.
        /// </summary>
        /// <param name="time"></param>
        /// <param name="shedule"></param>
        /// <returns>sorted array by descending</returns>
        private KeyValuePair<int, int>[] OrderLineByPriorityDescending (int time, int[,] shedule)
        {
            var priorities = new List<KeyValuePair<int, int>>();

            for (int i = 0; i < _times; i++)
            {
                var report = shedule[time, i];
                priorities.Add(new KeyValuePair<int, int>(report, i));
            }
            return priorities.OrderByDescending(elem => _ratings[elem.Key]).ToArray();
        }
    }
}
