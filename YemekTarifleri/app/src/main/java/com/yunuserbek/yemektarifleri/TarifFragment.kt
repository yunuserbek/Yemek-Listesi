package com.yunuserbek.yemektarifleri

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.media.Image
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_tarif.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream


class TarifFragment : Fragment() {
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    var selectedBitmap :Bitmap?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerLauncher()

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.fragment_tarif, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        button.setOnClickListener{
            kaydet(it)
        }
        imageView.setOnClickListener {
            gorselSec(it)
        }
        arguments?.let {
            var gelenbilgi =TarifFragmentArgs.fromBundle(it).bilgi
            if (gelenbilgi.equals("menudengeldim")){
                //yeni bir yemek eklemeye geldi
                yemekIsmiText.setText("")
                yemekMalzemeleriText.setText("")
                button.visibility = View.VISIBLE
                val gorselarkaplanı = BitmapFactory.decodeResource(context?.resources,R.drawable.selectedd)
                imageView.setImageBitmap(gorselarkaplanı)
                }else{
                    button.visibility =View.INVISIBLE
                    //daha önce olusturulan yemegi görmeye geldi
                val secilenId = TarifFragmentArgs.fromBundle(it).id
                context?.let {
                    try {
                        val db = it.openOrCreateDatabase("Yemekler",Context.MODE_PRIVATE,null)
                        val cursor = db.rawQuery("SELECT * FROM yemekler WHERE id = ?", arrayOf(secilenId.toString()))
                        val yemekismiIndex = cursor.getColumnIndex("yemekismi")
                        val yemekmalzemeIndext = cursor.getColumnIndex("yemekmalzemesi")
                        val yemekgorsel = cursor.getColumnIndex("gorsel")
                        while (cursor.moveToNext()){
                            yemekIsmiText.setText(cursor.getString(yemekismiIndex))
                            yemekMalzemeleriText.setText(cursor.getString(yemekmalzemeIndext))
                            val byteDizisi =cursor.getBlob(yemekgorsel)
                            val bitmap = BitmapFactory.decodeByteArray(byteDizisi,0,byteDizisi.size)
                            imageView.setImageBitmap(bitmap)
                        }
                        cursor.close()

                    }catch (e:Exception){
                        e.printStackTrace()
                    }
                }
            }
        }
    }
    fun kaydet(view:View){
        val yemekIsmi = yemekIsmiText.text.toString()
        val yemekMalzemesi = yemekMalzemeleriText.text.toString()
        if (selectedBitmap !=null){
            val kucukBitmap = kucukBitmapOlustur(selectedBitmap!!,300)

            val outputStream =ByteArrayOutputStream()
            kucukBitmap.compress(Bitmap.CompressFormat.PNG,50,outputStream)
            val byteDizisi =outputStream.toByteArray()

            try {
                context?.let {
                    val database =it.openOrCreateDatabase("Yemekler",Context.MODE_PRIVATE,null)
                    database.execSQL("CREATE TABLE IF NOT EXISTS yemekler(id INTEGER PRIMARY KEY,yemekismi VARCHAR,yemekmalzemesi VARCHAR,gorsel BLOB)")

                    val sqlString ="INSERT INTO yemekler(yemekismi,yemekmalzemesi,gorsel)VALUES(? ,? ,?)"
                    val statement = database.compileStatement(sqlString)
                    statement.bindString(1,yemekIsmi)
                    statement.bindString(2,yemekMalzemesi)
                    statement.bindBlob(3,byteDizisi)
                    statement.execute()
                }



            }catch (e:Exception){
                e.printStackTrace()
            }
            val action = TarifFragmentDirections.actionTarifFragmentToListeFragment()
            Navigation.findNavController(view).navigate(action)
        }



    }
    fun gorselSec(view: View){

        activity?.let {
            if (ContextCompat.checkSelfPermission(it.applicationContext,Manifest.permission.READ_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
                if (ActivityCompat.shouldShowRequestPermissionRationale(it,Manifest.permission.READ_EXTERNAL_STORAGE)){
                    Snackbar.make(view,"galeriye gitmek için izin lazım",Snackbar.LENGTH_INDEFINITE).setAction("izin ver",View.OnClickListener {
                        //izin isityecegiz
                        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)

                    }).show()


                }else{
                    //permission
                    permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)

                }

            }else{
                val intenTogGallery = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intenTogGallery)
            }
        }

            //izin verilmedi izin istememiz gerekiyor

    }
    private fun registerLauncher(){
        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
            if (result.resultCode==RESULT_OK){
                val intentFromResult = result.data
                if (intentFromResult !=null){
                    val imagedata = intentFromResult.data
                    //imageView.setImageURI(imagedata)
                    if (imagedata!=null){
                        try {
                            context?.let {
                                if (Build.VERSION.SDK_INT >=28){
                                    val source = ImageDecoder.createSource(it.contentResolver,imagedata)
                                    selectedBitmap = ImageDecoder.decodeBitmap(source)
                                    imageView.setImageBitmap(selectedBitmap)
                                }else{
                                    selectedBitmap = MediaStore.Images.Media.getBitmap(it.contentResolver,imagedata)
                                    imageView.setImageBitmap(selectedBitmap)
                                }
                            }



                        }catch (e:Exception){
                            e.printStackTrace()
                        }
                    }


                }
            }

        }
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){result->
            if (result){
                val intenTogGallery = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intenTogGallery)

            }else{
               Toast.makeText(context, "izne ihtiyacım var", Toast.LENGTH_LONG).show()
            }

        }

    }
    fun kucukBitmapOlustur(kullanicininSectigiBitmap: Bitmap, maximumBoyut: Int) : Bitmap {

        var width = kullanicininSectigiBitmap.width
        var height = kullanicininSectigiBitmap.height

        val bitmapOrani : Double = width.toDouble() / height.toDouble()

        if (bitmapOrani > 1) {
            // görselimiz yatay
            width = maximumBoyut
            val kisaltilmisHeight = width / bitmapOrani
            height = kisaltilmisHeight.toInt()
        } else {
            //görselimiz dikey
            height = maximumBoyut
            val kisaltilmisWidth = height * bitmapOrani
            width = kisaltilmisWidth.toInt()

        }


        return Bitmap.createScaledBitmap(kullanicininSectigiBitmap,width,height,true)
    }
}