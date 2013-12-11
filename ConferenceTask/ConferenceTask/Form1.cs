using System.Windows.Forms;

namespace ConferenceTask
{
    public partial class Form1 : Form
    {
        public Form1()
        {
            InitializeComponent();
            Generator.GenerateMatrix();
        }

        private void FillTable(Shedule shedule)
        {
            foreach (var report in shedule.Reports)
            {
                var control = new Label {Text = string.Format("{0}, id = {1}", report.Name, report.Id)};
                sheduleTable.Controls.Add(control, report.SectionNumber + 1, report.NumberInSection + 1);
            }

            for (int row = 1; row < 11; row++)
            {
                var control = new Label {Text = string.Format("{0}:00 - {1}:00", row + 9, row + 10)};
                sheduleTable.Controls.Add(control, 0, row);
            }
            for (int column = 1; column < 4; column++)
            {
                var control = new Label {Text = string.Format("Секция №{0}", column + 1)};
                sheduleTable.Controls.Add(control, column, 0);
            }
        }

        private void button1_Click_1(object sender, System.EventArgs e)
        {
//            TextBox textbox = new TextBox();
//            textbox.Text = "text";
//            sheduleTable.Controls.Add(new TextBox());
//            sheduleTable.SetCellPosition(textbox,new TableLayoutPanelCellPosition(2,2) );            
//            sheduleTable.SetCellPosition(new Control("bla-bla"), new TableLayoutPanelCellPosition(1,1));

            var dispatcher = new Dispatcher(Generator.ReadMatrixFromFile());

            FillTable(dispatcher.CreateShedule());
        }

        private void saveFileDialog1_FileOk(object sender, System.ComponentModel.CancelEventArgs e)
        {

        }

        private void sheduleTable_Paint(object sender, PaintEventArgs e)
        {

        }

        private void textBox1_TextChanged(object sender, System.EventArgs e)
        {

        }
    }
}
