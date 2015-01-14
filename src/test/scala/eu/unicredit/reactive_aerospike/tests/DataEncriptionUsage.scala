/* Copyright 2014 UniCredit S.p.A.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package eu.unicredit.reactive_aerospike.tests

import org.scalatest._
import eu.unicredit.reactive_aerospike.future.ScalaFactory.Helpers._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import eu.unicredit.reactive_aerospike.tests.model._
import com.aerospike.client.AerospikeClient

class DataEncriptionUsage extends FlatSpec {
  
	val poemText = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum"
	
	val newPoemText =
			   poemText+"MODIFIED"
		

    "I " should "work on my next poem and save it on 'unsafe' database " in {
	   
	   implicit val poemDao = PoemDao(Some("my really secret"))
	   
	   def poem(text: String) =   
	     new Poem("myPoem",
			   	  "Andrea",
			   	  text)
	     
	   val myPoem = poem(poemText) 
	   
	   try {
		   Await.result(poemDao.delete(myPoem.key), 100 millis)
	   } catch {
	     case _ : Throwable => 
	   }
	   
	   Await.result(poemDao.create(myPoem), 100 millis)
	   	   
	   val retP = Await.result(poemDao.read(myPoem.key), 100 millis)
	   
	   assert { myPoem.author == retP.author }
	   assert { poemText == retP.text }
	   	   
	   val modifiedPoem = poem(newPoemText)
	   
	   Await.result(poemDao.update(modifiedPoem.key, modifiedPoem), 100 millis)
	   
	   val retMP = Await.result(poemDao.read(myPoem.key), 100 millis)
	   
	   assert { modifiedPoem.author == retMP.author }
	   assert { newPoemText == retMP.text }
	   
	   assert { retP.text != retMP.text }
   }
    
   "Other users " should " be not able to read my work!" in {
	   implicit val poemDao = PoemDao(None)
	   
	   createSecondaryIndex(poemDao.client)

	   //I wont to steal wonderful Ansrea's poem!
	   val andreaPoems = Await.result(poemDao.findOn("author", "Andrea"), 100 millis)
	   
	   for {
	     andreaPoem <- andreaPoems
	   } yield {
	     assert { andreaPoem.text != poemText}
	     assert { andreaPoem.text != newPoemText}
	   }
   }
   
   def createSecondaryIndex(client: AerospikeClient) = {
     try {
    	 import com.aerospike.client.policy.Policy
    	 import com.aerospike.client.query.IndexType
    	 val policy = new Policy
    	 policy.timeout = 0; // Do not timeout on index create.
    	 val task = client.createIndex(policy, "test", "poems", "poemAuthor", "author", IndexType.STRING);
    	 task.waitTillComplete();
     } catch {
       case err: Throwable =>
         err.printStackTrace()
     }
   }
  
}
