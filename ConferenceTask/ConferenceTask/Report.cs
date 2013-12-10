using System;

namespace ConferenceTask
{
    public class Report
    {
        //имя доклада. Генирируется Великим Алгоритмом
        public string Name { get; set; }
        //номер секции, в которой находится доклад
        public int SectionNumber { get; set; }
        //порядковый номер доклада в секции
        public int NumberInSection { get; set; }
        //общий порядковый номер доклада
        public int Id { get; set; }
    }
}
