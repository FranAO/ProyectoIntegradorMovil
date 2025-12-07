package com.example.proyectointegrador.Database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.Nullable;

import com.example.proyectointegrador.Models.Student;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class DBHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "ValleRouteDB";
    private static final int DATABASE_VERSION = 2;
    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);

    public DBHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE students (" +
                "id TEXT PRIMARY KEY, " +
                "firstName TEXT, " +
                "lastName TEXT, " +
                "institutionalEmail TEXT UNIQUE, " +
                "phone TEXT, " +
                "passwordHash TEXT, " +
                "role TEXT, " +
                "createdAt TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public boolean insertarStudent(Student student) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id", student.getId());
        values.put("firstName", student.getFirstName());
        values.put("lastName", student.getLastName());
        values.put("institutionalEmail", student.getInstitutionalEmail());
        values.put("phone", student.getPhone());
        values.put("passwordHash", student.getPaswordHash());
        values.put("role", student.getRole());
        values.put("createdAt", student.getCreatedAt() != null ? dateFormatter.format(student.getCreatedAt()) : null);

        long resultado = db.insert("students", null, values);
        return resultado != -1;
    }

    public boolean editarStudent(Student student) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("firstName", student.getFirstName());
        values.put("lastName", student.getLastName());
        values.put("institutionalEmail", student.getInstitutionalEmail());
        values.put("phone", student.getPhone());
        values.put("passwordHash", student.getPaswordHash());
        values.put("role", student.getRole());
        values.put("createdAt", student.getCreatedAt() != null ? dateFormatter.format(student.getCreatedAt()) : null);

        int editar = db.update("students", values, "id = ?", new String[] { student.getId() });
        return editar > 0;
    }

    public boolean eliminarStudent(String id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int eliminar = db.delete("students", "id = ?", new String[] { id });
        return eliminar > 0;
    }

    public ArrayList<Student> obtenerStudents() {
        ArrayList<Student> lista = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM students", null);
        if (cursor.moveToFirst()) {
            do {
                Date createdAt = null;
                try {
                    if (cursor.getString(7) != null) {
                        createdAt = dateFormatter.parse(cursor.getString(7));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Student student = new Student(
                        cursor.getString(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getString(4),
                        cursor.getString(5),
                        cursor.getString(6),
                        createdAt);
                lista.add(student);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return lista;
    }

    public Student obtenerStudentPorEmail(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM students WHERE institutionalEmail = ?", new String[] { email });
        Student student = null;
        if (cursor.moveToFirst()) {
            Date createdAt = null;
            try {
                if (cursor.getString(7) != null) {
                    createdAt = dateFormatter.parse(cursor.getString(7));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            student = new Student(
                    cursor.getString(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3),
                    cursor.getString(4),
                    cursor.getString(5),
                    cursor.getString(6),
                    createdAt);
        }
        cursor.close();
        return student;
    }
}
