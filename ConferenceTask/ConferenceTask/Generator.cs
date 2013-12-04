using System;
using System.IO;

namespace ConferenceTask
{
    class Generator
    {
        /// <summary>
        /// количество слушателей
        /// </summary>
        public const int Listeners = 100;
        /// <summary>
        /// количество потоков
        /// </summary>
        public const int Flows = 3;
        /// <summary>
        /// количество докладов
        /// </summary>
        public const int Topics = 30;
        /// <summary>
        /// путь к файлу с входной матрицей
        /// </summary>
        public const string FilePath = "input.txt";

        private static readonly int[,] Matrix = new int[100,30];


        public static readonly string[] Separator = new[]{" "};

        /// <summary>
        /// генерирует и записывает матрицу в файл
        /// </summary>
        public static void GenerateMatrix()
        {
            var random = new Random();
            using (var writer = new StreamWriter(FilePath))
            {
                for (int listener = 0; listener < Listeners; listener++)
                {
                    for (int topic = 0; topic < Topics; topic++)
                    {
                        writer.Write("{0}", random.Next(10) + Separator[0]);
                    }
                    writer.WriteLine();
                }
            }
        }

        /// <summary>
        /// считывает матрицу из файла
        /// </summary>
        /// <returns>
        /// входная матрица из файла
        /// </returns>
        public static int[,] ReadMatrixFromFile()
        {
            var matrix = new int[Listeners,Topics];
            using (var reader = new StreamReader(FilePath))
            {
                for (int listener = 0; listener < Listeners; listener++)
                {
                    string line;
                    if ((line = reader.ReadLine()) == null) throw new Exception("Incorrect file!");
                    var inputArray = line.Split(Separator, StringSplitOptions.RemoveEmptyEntries);
                    if(inputArray.Length != Topics) throw  new Exception("Incorrect file!");
                    for (int topic = 0; topic < Topics; topic++)
                    {
                        int res;
                        int.TryParse(inputArray[topic], out res);
                        matrix[listener, topic] = res;
                    }
                }
            }
            return matrix;
        }
    }
}
