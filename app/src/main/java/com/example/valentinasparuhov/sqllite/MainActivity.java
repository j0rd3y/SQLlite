package com.example.valentinasparuhov.sqllite;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private EditText editTxtFirstName;
    private EditText editTxtLastName;
    private EditText editTxtFacNom;
    private Button btn;
    private ListView list;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> arrayList;
    private int studentPositionInLV;
    public static SQLiteDatabase mydatabase;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mydatabase = openOrCreateDatabase("Students", MODE_PRIVATE, null);

        editTxtFirstName = (EditText) findViewById(R.id.teFirstName);
        editTxtLastName = (EditText) findViewById(R.id.teLastName);
        editTxtFacNom = (EditText) findViewById(R.id.teFacNom);
        btn = (Button) findViewById(R.id.btnAdd);
        list = (ListView) findViewById(R.id.lvStudents);
        arrayList = new ArrayList<String>();

        // Adapter: You need three parameters 'the context, id of the layout (it will be where the data is shown),
        // and the array that contains the data
        adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_item, arrayList);

        DBPrepare();
        fillListView();

        //SYZDAVANE NA EVENT ZA KLIKVANE NA BUTON ADD STUDENT
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String firstName = editTxtFirstName.getText().toString();
                String lastName = editTxtLastName.getText().toString();
                String facNom = editTxtFacNom.getText().toString();

                if (facNom.isEmpty() | firstName.isEmpty() | lastName.isEmpty()) {
                    AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                    alertDialog.setTitle("Missing information.");
                    alertDialog.setMessage("All the fields need to be filled out to add a student.");
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    alertDialog.show();
                }else {
                    // this line adds the data of your EditText and puts in your array
                    arrayList.add(firstName + " " + lastName + " " + facNom);
                    // next thing you have to do is check if your adapter has changed
                    adapter.notifyDataSetChanged();

                    mydatabase.execSQL("INSERT INTO Students(FirstName,LastName,FacNom) VALUES('"+firstName+"','"+lastName+"','"+facNom+"');");
                    editTxtFirstName.getText().clear();
                    editTxtLastName.getText().clear();
                    editTxtFacNom.getText().clear();
                    fillListView();
                }
            }
        });
        //KRAI NA EVENT ZA KLIK NA BUTON ADD STUDENT

        //SYZDAVANE NA EVENT ZA KLIK NA ITEM NA STUDENT
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> myAdapter, View myView, int myItemInt, long mylng) {
                String selectedFromList =(String) (list.getItemAtPosition(myItemInt));
                String[] splited = selectedFromList.split("\\s+");
                String finalMessage = splited[2] + " " +splited[3] + ", Faculty number : " + " " + splited[4] + ", has the following grades : " ;
                Cursor
                        resultSet = mydatabase.rawQuery("Select Students.FirstName, Students.LastName, Grades.Name, StudentGrades.recievedGrade from StudentGrades "+
                                " INNER JOIN Students\n" +
                                    " ON StudentGrades.studentID = Students.id"+
                                " INNER JOIN Grades\n" +
                                    " ON StudentGrades.gradeID = Grades.id" +
                                " WHERE StudentGrades.studentID = '"+splited[0]+"'"
                        ,null);
                //Cursor resultSet = mydatabase.rawQuery("Select * from StudentGrades",null);

                try {
                    while (resultSet.moveToNext()) {
                        finalMessage+= "\n"+ resultSet.getString(3) + " in " + resultSet.getString(2);
                    }
                } finally {
                    resultSet.close();
                }

                studentPositionInLV = myItemInt;

                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                alertDialog.setTitle("Student.");
                alertDialog.setMessage(finalMessage);
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            }
        });
        //KRAI NA EVENT ZA KLIK NA ITEM NA

        //SYZDAVANE NA EVENT ZA prodyljitelno klikvane na item v List view-to
        list.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
                AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) contextMenuInfo;
                studentPositionInLV = info.position;
                menu.setHeaderTitle("Choose option");
                menu.add(R.id.lvStudents, view.getId(), 0, "Manage grades");
                menu.add(R.id.lvStudents, view.getId(), 1, "Delete");
                menu.add(R.id.lvStudents, view.getId(), 2, "Cancel");
            }
        });
        //KRAI NA SYZDAVANE NA EVENT ZA prodyljitelno klikvane na item v List view-to
    }


    public boolean onContextItemSelected(MenuItem item) {
        String[] selectedFromList = (list.getItemAtPosition(studentPositionInLV)).toString().split("\\s+");

        if (item.getTitle().equals("Manage grades")) {
            mydatabase.close();
            Intent myIntent = new Intent(MainActivity.this, gradeManipulation.class);
            myIntent.putExtra("key", selectedFromList[0]); //Optional parameters
            MainActivity.this.startActivity(myIntent);
            mydatabase = openOrCreateDatabase("Students", MODE_PRIVATE, null);
        }else if (item.getTitle().equals("Delete")){

            mydatabase.delete("StudentGrades","studentID=?",new String[]{selectedFromList[0]});
            mydatabase.delete("Students","id=?",new String[]{selectedFromList[0]});
            fillListView();
        }
        return true;
    }

    public void fillListView(){
        adapter.clear();

        // Here, you set the data in your ListView
        list.setAdapter(adapter);
        //Popylvane na list view sys zapisi ot bazata danni
        Cursor resultSet = mydatabase.rawQuery("Select * from Students",null);
        try {
            while (resultSet.moveToNext()) {
                // this line adds the data of your EditText and puts in your array
                arrayList.add(resultSet.getString(0) + " : " + resultSet.getString(1) + " " + resultSet.getString(2) + " " + resultSet.getString(3));
            }
            // next thing you have to do is check if your adapter has changed
            adapter.notifyDataSetChanged();
        } finally {
            resultSet.close();
        }
    }

    public void DBPrepare(){
        mydatabase.execSQL("CREATE TABLE IF NOT EXISTS Students(id INTEGER PRIMARY KEY,FirstName VARCHAR,LastName VARCHAR, FacNom VARCHAR);");
        mydatabase.execSQL("CREATE TABLE IF NOT EXISTS Grades(id INTEGER PRIMARY KEY,Name VARCHAR);");
        mydatabase.execSQL("CREATE TABLE IF NOT EXISTS StudentGrades(id INTEGER PRIMARY KEY, studentID INTEGER, gradeID INTEGER, recievedGrade REAL,"+
                " FOREIGN KEY ('studentID') REFERENCES 'Students'('id'),"+
                " FOREIGN KEY ('gradeID') REFERENCES 'Grades'('id'));");

        //Popylvane na list view sys zapisi ot bazata danni
        Cursor resultSet = mydatabase.rawQuery("Select * from Grades",null);
        try {
            int gradeCount = resultSet.getCount();
            resultSet.close();

            //mydatabase.delete("StudentGrades","id>?",new String[]{"0"});
            //mydatabase.delete("Grades","id>?",new String[]{"0"});
            //mydatabase.delete("Students","id>?",new String[]{"0"});

            if (gradeCount<4){
                mydatabase.delete("Grades","id>?",new String[]{"0"});
                mydatabase.execSQL("INSERT INTO Grades(Name) VALUES('Computer science');");
                mydatabase.execSQL("INSERT INTO Grades(Name) VALUES('Math');");
                mydatabase.execSQL("INSERT INTO Grades(Name) VALUES('Database systems');");
                mydatabase.execSQL("INSERT INTO Grades(Name) VALUES('Java programing');");

                //mydatabase.execSQL("INSERT INTO Students(FirstName,LastName,FacNom) VALUES('Valentin','Asparuhov','9402140980');");
                //mydatabase.execSQL("INSERT INTO StudentGrades(studentID,gradeID,recievedGrade) VALUES(1,1,5.50);");
                //mydatabase.execSQL("INSERT INTO StudentGrades(studentID,gradeID,recievedGrade) VALUES(1,2,5.00);");
                //mydatabase.execSQL("INSERT INTO StudentGrades(studentID,gradeID,recievedGrade) VALUES(1,3,6.00);");
                //mydatabase.execSQL("INSERT INTO StudentGrades(studentID,gradeID,recievedGrade) VALUES(2,4,4.00);");
            }
        } finally {
            resultSet.close();
        }
    }

    @Override
    public void onBackPressed() {
        mydatabase.close();
        finish();
        return;
    }
}
