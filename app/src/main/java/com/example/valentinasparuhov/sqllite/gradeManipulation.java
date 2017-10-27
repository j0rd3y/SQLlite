package com.example.valentinasparuhov.sqllite;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;

public class gradeManipulation extends AppCompatActivity {
    private String studentID;
    private EditText etGradeValue;
    private EditText etStudentName;
    private Spinner spGradeName;
    private ListView list;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> arrayList;
    private Button btn;
    private SQLiteDatabase mydatabase;
    private int studentPositionInLV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_grade_manipulation);

        mydatabase = openOrCreateDatabase("Students", MODE_PRIVATE, null);

        etGradeValue = (EditText) findViewById(R.id.etGrade);
        etStudentName = (EditText) findViewById(R.id.etName);
        btn = (Button) findViewById(R.id.btnInsert);
        list = (ListView) findViewById(R.id.lvGrades);
        spGradeName = (Spinner) findViewById(R.id.spSubject);
        arrayList = new ArrayList<String>();
        adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_item, arrayList);
        list.setAdapter(adapter);

        //SYZDAVANE NA EVENT ZA KLIKVANE NA BUTON ADD STUDENT
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String gradeValue = etGradeValue.getText().toString();
                    TextView textView = (TextView) spGradeName.getSelectedView();
                    String result = textView.getText().toString();

                    if (gradeValue.isEmpty() | result.isEmpty()) {
                        AlertDialog alertDialog = new AlertDialog.Builder(gradeManipulation.this).create();
                        alertDialog.setTitle("Missing information.");
                        alertDialog.setMessage("All the fields need to be filled out to add a grade to a student.");
                        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                        alertDialog.show();
                    } else if (checkGrade(gradeValue)) {
                        AlertDialog alertDialog = new AlertDialog.Builder(gradeManipulation.this).create();
                        alertDialog.setTitle("Incorrect grade.");
                        alertDialog.setMessage("The grade must be >=2 AND <=6.");
                        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                        alertDialog.show();
                    }else{
                        Cursor resultSet = mydatabase.rawQuery("Select * FROM Grades WHERE Name = '" + result + "'", null);//getting the id of the subject
                        resultSet.moveToNext();

                        ContentValues values = new ContentValues();

                        values.put("studentID", Integer.parseInt(studentID));
                        values.put("gradeID", resultSet.getString(0));
                        values.put("recievedGrade", Double.parseDouble(gradeValue));

                        try{
                            long a = mydatabase.insertOrThrow("StudentGrades",null,values);
                        }catch(Exception e){
                            AlertDialog alertDialog = new AlertDialog.Builder(gradeManipulation.this).create();
                            alertDialog.setTitle("Error.");
                            alertDialog.setMessage(e.getMessage().toString());
                            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    });
                            alertDialog.show();
                        }
                        fillListView();
                        etGradeValue.getText().clear();
                        spGradeName.setSelection(0);
                        resultSet.close();
                    }
                }
        });
        //KRAI NA EVENT ZA KLIK NA BUTON ADD STUDENT

        //SYZDAVANE NA EVENT ZA prodyljitelno klikvane na item v List view-to
        list.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
                AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) contextMenuInfo;
                studentPositionInLV = info.position;
                menu.setHeaderTitle("Choose option");
                menu.add(R.id.lvStudents, view.getId(), 0, "Delete");
                menu.add(R.id.lvStudents, view.getId(), 1, "Cancel");
            }
        });
        //KRAI NA SYZDAVANE NA EVENT ZA prodyljitelno klikvane na item v List view-to

        Intent intent = getIntent();
        studentID = intent.getStringExtra("key"); //if it's a string you stored.

        //Vzemane na ime na student i vkarvane v TE
        Cursor resultSet = mydatabase.rawQuery("Select FirstName,LastName from Students WHERE id = '"+studentID+"'",null);
        //resultSet.moveToNext();
        try {
            while (resultSet.moveToNext()) {
                etStudentName.setText(resultSet.getString(0)+" " + resultSet.getString(1));
            }
        } finally {
            resultSet.close();
        }
        fillListView();
        //Vzemane na imena na predmeti spinner
        resultSet = mydatabase.rawQuery("SELECT * FROM Grades;",null);
        ArrayList<String>  arrayListSP = new ArrayList<String>();
        ArrayAdapter<String> adapterSP = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_item, arrayListSP);
        spGradeName.setAdapter(adapterSP);
        try {
            while (resultSet.moveToNext()) {
                // this line adds the data of your EditText and puts in your array
                arrayListSP.add(resultSet.getString(1));
            }
            // next thing you have to do is check if your adapter has changed
            adapterSP.notifyDataSetChanged();
        } finally {
            resultSet.close();
        }
    }

    public boolean onContextItemSelected(MenuItem item) {
        String[] selectedFromList = (list.getItemAtPosition(studentPositionInLV)).toString().split("\\s+");

        if (item.getTitle().equals("Delete")){
            mydatabase.delete("StudentGrades","id=?",new String[]{selectedFromList[0]});
            fillListView();
        }
        return true;
    }

    private boolean checkGrade(String grade){
        try{
            double foo = Double.parseDouble(grade);
            if (foo>=2 & foo<=6){
                return false;
            }else{
                return true;
            }
        }catch(Exception e){
            return false;
        }
    }

    private void fillListView(){
        adapter.clear();

        // Here, you set the data in your ListView
        list.setAdapter(adapter);
        //Popylvane na list view sys zapisi ot bazata danni
        //vzimane na vsi4ki ocenki za tozi student
        Cursor resultSet = mydatabase.rawQuery("Select Grades.Name, StudentGrades.recievedGrade, StudentGrades.id from StudentGrades "+
                        " INNER JOIN Grades\n" +
                        " ON StudentGrades.gradeID = Grades.id" +
                        " WHERE StudentGrades.studentID = '"+studentID+"'"
                ,null);

        try {
            while (resultSet.moveToNext()) {
                // this line adds the data of your EditText and puts in your array
                arrayList.add(resultSet.getString(2) + " : " + resultSet.getString(1) + " at " + resultSet.getString(0));
            }
            // next thing you have to do is check if your adapter has changed
            adapter.notifyDataSetChanged();
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
