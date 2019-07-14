import net.runelite.mapping.Export;
import net.runelite.mapping.Implements;
import net.runelite.mapping.ObfuscatedGetter;
import net.runelite.mapping.ObfuscatedName;
import net.runelite.mapping.ObfuscatedSignature;

@ObfuscatedName("hn")
@Implements("IsaacCipher")
public final class IsaacCipher {
   @ObfuscatedName("w")
   @ObfuscatedGetter(
      intValue = 1514845493
   )
   @Export("valuesRemaining")
   int valuesRemaining;
   @ObfuscatedName("o")
   @Export("results")
   int[] results;
   @ObfuscatedName("u")
   @Export("mm")
   int[] mm;
   @ObfuscatedName("g")
   @ObfuscatedGetter(
      intValue = -1670027699
   )
   @Export("aa")
   int aa;
   @ObfuscatedName("l")
   @ObfuscatedGetter(
      intValue = -325762649
   )
   @Export("bb")
   int bb;
   @ObfuscatedName("e")
   @ObfuscatedGetter(
      intValue = -1951204929
   )
   @Export("cc")
   int cc;

   public IsaacCipher(int[] seed) {
      this.mm = new int[256];
      this.results = new int[256];

      for (int var2 = 0; var2 < seed.length; ++var2) {
         this.results[var2] = seed[var2];
      }

      this.method139();
   }

   @ObfuscatedName("m")
   @ObfuscatedSignature(
      signature = "(I)I",
      garbageValue = "-7509790"
   )
   @Export("nextInt")
   final int nextInt() {
      if (0 == --this.valuesRemaining + 1) {
         this.generateMoreResults();
         this.valuesRemaining = 255;
      }

      return this.results[this.valuesRemaining];
   }

   @ObfuscatedName("f")
   @ObfuscatedSignature(
      signature = "(I)I",
      garbageValue = "-506873526"
   )
   final int method137() {
      if (this.valuesRemaining == 0) {
         this.generateMoreResults();
         this.valuesRemaining = 256;
      }

      return this.results[this.valuesRemaining - 1];
   }

   @ObfuscatedName("q")
   @ObfuscatedSignature(
      signature = "(B)V",
      garbageValue = "124"
   )
   @Export("generateMoreResults")
   final void generateMoreResults() {
      this.bb += ++this.cc;

      for (int var1 = 0; var1 < 256; ++var1) {
         int var2 = this.mm[var1];
         if ((var1 & 2) == 0) {
            if ((var1 & 1) == 0) {
               this.aa ^= this.aa << 13;
            } else {
               this.aa ^= this.aa >>> 6;
            }
         } else if ((var1 & 1) == 0) {
            this.aa ^= this.aa << 2;
         } else {
            this.aa ^= this.aa >>> 16;
         }

         this.aa += this.mm[128 + var1 & 255];
         int var3;
         this.mm[var1] = var3 = this.mm[(var2 & 1020) >> 2] + this.bb + this.aa;
         this.results[var1] = this.bb = this.mm[(var3 >> 8 & 1020) >> 2] + var2;
      }

   }

   @ObfuscatedName("w")
   @ObfuscatedSignature(
      signature = "(I)V",
      garbageValue = "579890110"
   )
   final void method139() {
      int var1 = 0x9e3779b9;
      int var2 = 0x9e3779b9;
      int var3 = 0x9e3779b9;
      int var4 = 0x9e3779b9;
      int var5 = 0x9e3779b9;
      int var6 = 0x9e3779b9;
      int var7 = 0x9e3779b9;
      int var8 = 0x9e3779b9;

      int var9;
      for (var9 = 0; var9 < 4; ++var9) {
         var8 ^= var7 << 11;
         var5 += var8;
         var7 += var6;
         var7 ^= var6 >>> 2;
         var4 += var7;
         var6 += var5;
         var6 ^= var5 << 8;
         var3 += var6;
         var5 += var4;
         var5 ^= var4 >>> 16;
         var2 += var5;
         var4 += var3;
         var4 ^= var3 << 10;
         var1 += var4;
         var3 += var2;
         var3 ^= var2 >>> 4;
         var8 += var3;
         var2 += var1;
         var2 ^= var1 << 8;
         var7 += var2;
         var1 += var8;
         var1 ^= var8 >>> 9;
         var6 += var1;
         var8 += var7;
      }

      for (var9 = 0; var9 < 256; var9 += 8) {
         var8 += this.results[var9];
         var7 += this.results[var9 + 1];
         var6 += this.results[var9 + 2];
         var5 += this.results[var9 + 3];
         var4 += this.results[var9 + 4];
         var3 += this.results[var9 + 5];
         var2 += this.results[var9 + 6];
         var1 += this.results[var9 + 7];
         var8 ^= var7 << 11;
         var5 += var8;
         var7 += var6;
         var7 ^= var6 >>> 2;
         var4 += var7;
         var6 += var5;
         var6 ^= var5 << 8;
         var3 += var6;
         var5 += var4;
         var5 ^= var4 >>> 16;
         var2 += var5;
         var4 += var3;
         var4 ^= var3 << 10;
         var1 += var4;
         var3 += var2;
         var3 ^= var2 >>> 4;
         var8 += var3;
         var2 += var1;
         var2 ^= var1 << 8;
         var7 += var2;
         var1 += var8;
         var1 ^= var8 >>> 9;
         var6 += var1;
         var8 += var7;
         this.mm[var9] = var8;
         this.mm[var9 + 1] = var7;
         this.mm[var9 + 2] = var6;
         this.mm[var9 + 3] = var5;
         this.mm[var9 + 4] = var4;
         this.mm[var9 + 5] = var3;
         this.mm[var9 + 6] = var2;
         this.mm[var9 + 7] = var1;
      }

      for (var9 = 0; var9 < 256; var9 += 8) {
         var8 += this.mm[var9];
         var7 += this.mm[var9 + 1];
         var6 += this.mm[var9 + 2];
         var5 += this.mm[var9 + 3];
         var4 += this.mm[var9 + 4];
         var3 += this.mm[var9 + 5];
         var2 += this.mm[var9 + 6];
         var1 += this.mm[var9 + 7];
         var8 ^= var7 << 11;
         var5 += var8;
         var7 += var6;
         var7 ^= var6 >>> 2;
         var4 += var7;
         var6 += var5;
         var6 ^= var5 << 8;
         var3 += var6;
         var5 += var4;
         var5 ^= var4 >>> 16;
         var2 += var5;
         var4 += var3;
         var4 ^= var3 << 10;
         var1 += var4;
         var3 += var2;
         var3 ^= var2 >>> 4;
         var8 += var3;
         var2 += var1;
         var2 ^= var1 << 8;
         var7 += var2;
         var1 += var8;
         var1 ^= var8 >>> 9;
         var6 += var1;
         var8 += var7;
         this.mm[var9] = var8;
         this.mm[var9 + 1] = var7;
         this.mm[var9 + 2] = var6;
         this.mm[var9 + 3] = var5;
         this.mm[var9 + 4] = var4;
         this.mm[var9 + 5] = var3;
         this.mm[var9 + 6] = var2;
         this.mm[var9 + 7] = var1;
      }

      this.generateMoreResults();
      this.valuesRemaining = 256;
   }

   @ObfuscatedName("m")
   @ObfuscatedSignature(
      signature = "(Lir;Lir;I)V",
      garbageValue = "75867683"
   )
   @Export("setNpcDefinitionArchives")
   public static void setNpcDefinitionArchives(AbstractArchive var0, AbstractArchive var1) {
      NPCDefinition.NpcDefinition_archive = var0;
      NPCDefinition.NpcDefinition_modelArchive = var1;
   }

   @ObfuscatedName("fl")
   @ObfuscatedSignature(
      signature = "(II)V",
      garbageValue = "1676957578"
   )
   static void method4093(int var0) {
      if (var0 == -3) {
         class54.setLoginResponseString("Connection timed out.", "Please try using a different world.", "");
      } else if (var0 == -2) {
         class54.setLoginResponseString("", "Error connecting to server.", "");
      } else if (var0 == -1) {
         class54.setLoginResponseString("No response from server.", "Please try using a different world.", "");
      } else if (var0 == 3) {
         Login.loginIndex = 3;
         Login.field467 = 1;
      } else if (var0 == 4) {
         class32.method578(0);
      } else if (var0 == 5) {
         Login.field467 = 2;
         class54.setLoginResponseString("Your account has not logged out from its last", "session or the server is too busy right now.", "Please try again in a few minutes.");
      } else if (var0 != 68 && (Client.onMobile || var0 != 6)) {
         if (var0 == 7) {
            class54.setLoginResponseString("This world is full.", "Please use a different world.", "");
         } else if (var0 == 8) {
            class54.setLoginResponseString("Unable to connect.", "Login server offline.", "");
         } else if (var0 == 9) {
            class54.setLoginResponseString("Login limit exceeded.", "Too many connections from your address.", "");
         } else if (var0 == 10) {
            class54.setLoginResponseString("Unable to connect.", "Bad session id.", "");
         } else if (var0 == 11) {
            class54.setLoginResponseString("We suspect someone knows your password.", "Press 'change your password' on front page.", "");
         } else if (var0 == 12) {
            class54.setLoginResponseString("You need a members account to login to this world.", "Please subscribe, or use a different world.", "");
         } else if (var0 == 13) {
            class54.setLoginResponseString("Could not complete login.", "Please try using a different world.", "");
         } else if (var0 == 14) {
            class54.setLoginResponseString("The server is being updated.", "Please wait 1 minute and try again.", "");
         } else if (var0 == 16) {
            class54.setLoginResponseString("Too many login attempts.", "Please wait a few minutes before trying again.", "");
         } else if (var0 == 17) {
            class54.setLoginResponseString("You are standing in a members-only area.", "To play on this world move to a free area first", "");
         } else if (var0 == 18) {
            class32.method578(1);
         } else if (var0 == 19) {
            class54.setLoginResponseString("This world is running a closed Beta.", "Sorry invited players only.", "Please use a different world.");
         } else if (var0 == 20) {
            class54.setLoginResponseString("Invalid loginserver requested.", "Please try using a different world.", "");
         } else if (var0 == 22) {
            class54.setLoginResponseString("Malformed login packet.", "Please try again.", "");
         } else if (var0 == 23) {
            class54.setLoginResponseString("No reply from loginserver.", "Please wait 1 minute and try again.", "");
         } else if (var0 == 24) {
            class54.setLoginResponseString("Error loading your profile.", "Please contact customer support.", "");
         } else if (var0 == 25) {
            class54.setLoginResponseString("Unexpected loginserver response.", "Please try using a different world.", "");
         } else if (var0 == 26) {
            class54.setLoginResponseString("This computers address has been blocked", "as it was used to break our rules.", "");
         } else if (var0 == 27) {
            class54.setLoginResponseString("", "Service unavailable.", "");
         } else if (var0 == 31) {
            class54.setLoginResponseString("Your account must have a displayname set", "in order to play the game.  Please set it", "via the website, or the main game.");
         } else if (var0 == 32) {
            class54.setLoginResponseString("Your attempt to log into your account was", "unsuccessful.  Don't worry, you can sort", "this out by visiting the billing system.");
         } else if (var0 == 37) {
            class54.setLoginResponseString("Your account is currently inaccessible.", "Please try again in a few minutes.", "");
         } else if (var0 == 38) {
            class54.setLoginResponseString("You need to vote to play!", "Visit runescape.com and vote,", "and then come back here!");
         } else if (var0 == 55) {
            Login.loginIndex = 8;
         } else {
            if (var0 == 56) {
               class54.setLoginResponseString("Enter the 6-digit code generated by your", "authenticator app.", "");
               GameShell.updateGameState(11);
               return;
            }

            if (var0 == 57) {
               class54.setLoginResponseString("The code you entered was incorrect.", "Please try again.", "");
               GameShell.updateGameState(11);
               return;
            }

            if (var0 == 61) {
               Login.loginIndex = 7;
            } else {
               class54.setLoginResponseString("Unexpected server response", "Please try using a different world.", "");
            }
         }
      } else {
         class54.setLoginResponseString("RuneScape has been updated!", "Please reload this page.", "");
      }

      GameShell.updateGameState(10);
   }
}