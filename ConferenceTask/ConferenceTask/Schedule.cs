using System.Collections.Generic;

namespace ConferenceTask
{
    public class Schedule
    {
        //количество секций
        public const int Sections = Generator.Sections;
        //количество докладов в одной секции
        public const int ReportsCountInSection = Generator.Reports / Generator.Sections;
        //общее количество докладов
        public const int ReportsCount = Sections*ReportsCountInSection;

        //список докладов
        public List<Report> Reports { get; set; }

        public Schedule()
        {
            Reports = new List<Report>();
        }
    }
}
