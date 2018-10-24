# tof
Transfer Ownership Folder - Auto transfer ownership from author to a designated account which could be a service account or a regular account

The background is that many domain users created a scenario that individual domain users were creating shared files, which are permanently owned by the original creator. Domain administrators wants any such files to be owned by a central account. This could be a single central admin account for the domain, or there could be many such accounts, one for each project. 
This Java app handles the automatic transfer of ownership of designated files from a human user account to an admin/archive account. 
It will do this for multiple domains in a single invocation, based on the configuration file. Users designate files for TOF, by placing their parent folder under 
a master folder, referred to as ALLTOFS. Nb. the ALLTOFS folder can actually have any name, as it is identified by its file ID within the config file, rather than by name.

## Installation
  1. You must install a Jave Runtime Environment (JRE) from http://www.oracle.com/technetwork/java/javase/downloads/jre8-downloads-2133155.html
  1. Unzip the distribution into a directory, called say TOF.
 


## Configuration
For each domain that you wish to TOF, there are a few setup and configuration steps to be performed. This part is a bit fiddly, but only needs
doing once per domain. All steps should be performed while logged in as an admin user of the domain.

#### Google stuff

  1. Go to the Google API Console at https://console.developers.google.com/project
  
  1. Double check the avatar on the top right to ensure you are running as the domain admin.
  
  1. Click Create Project and fill in the form
  
  1. Once the project is created, go to APIs & Auth / APIs 
  1. Enable the Drive API
  1. Go to APIs & Auth / Credentials
  1. Create a new Client Id
  1. Select Service Account and Create P12 Key
  1. This will download a file with an extension of .p12 . Rename this file to domain.p12, where domain is the domain name with full-stops stripped out, eg. mydomaincom.p12. 
  Move the file into the root folder of TOF
  1. Make a note of the Service Account Email Address, eg. 617103065-q3gm0il1mmc6f83kkuv9n6m92g8@developer.gserviceaccount.com  [1]
  1. Make a note of the Client ID eg. 498909596280-hg2d77klsgjm5g7dall0mmtfr2b.apps.googleusercontent.com [2]
  1. Go into the Admin settings for the domain , eg. https://admin.google.com/MYDOMAIN.com/AdminHome
  1. Security / Show more / Advanced settings / Manage API Clients  (you can probably go directly to https://admin.google.com/MYDOMAIN.com/AdminHome?chromeless=1#OGX:ManageOauthClients)
  1. Under Client Name, paste the Client ID from [2]
  1. Under One or More API Scopes, enter https://www.googleapis.com/auth/drive 
  1. Click Authorize
  1. Go to drive.google.com
  1. Check that you are the admin user and in the correct Drive domain
  1. Create the ALLTOFS folder. You can call it anything, but go into it so you can note the folder ID from the URL [3] (you can also change its colour if you wish, by right clicking)
  1. Change the permissions of ALLTOFS so it is writeable by all members of the domain. (You could only make it writeable by individual project managers - your choice)
  1. Also Share it with the Service Account Email from [1]
  
  That's all the Google config done. What you've done is basically create a Service Account and delegate domain wide authority to that Service Account. 
  You've then created the master ALLTOFS folder and shared it with the project managers and with the service account. In theory, you could actually use the same 
  Service Account for all domains, but I prefer to create a separate Service Account for each domain for quota purposes.
  
  Next you need to configure all of this information into TOF.

#### TOF stuff
  
  1. Edit the tof.cfg file
  1. Replace "primetext.com" with your domain name
  1. Replace the serviceAccountEmail with the email from [1]
  1. Replace the tofsFolderId with the folderId from [3]
  1. Replace "gwappo@primetext.com" with the email address of the admin/archive user, eg. admin@,mydomain.com
  1. Save and exhale
  
If you wish to run TOF against a second domain, repeat the Google stuff for the new domain. In the tof.cfg file, duplicate the four domain lines and paste in the appropriate values.


## Running

I've provided a *nix shell script called tof.sh, which you can run. I'm not a Windows guy, but the windows equivalent should be pretty straightforward.
from the TOF directory created by unzipping the archive, just run `tof.sh`. tof.cfg must also be in this directory.`

The output should look something like this ..,

    [M216] reading tof.cfg

    [M60] Processing domain primetext.com ...

    [M114] Authorising Drive Service for the Service Account ...
    
    [M111] checking access to 'ALL TOFS' (0B7oUW_j-nM7kfjFXWXd6cmdxbjltNGlfRHhnd3QyMmk2cnQwblNOSnJwdWpXdWZPNjBTMU0)...
    
    [M115] Listing child folders of 'ALL TOFS' (0B7oUW_j-nM7kfjFXWXd6cmdxbjltNGlfRHhnd3QyMmk2cnQwblNOSnJwdWpXdWZPNjBTMU0)...
    
        [M145] Processing child 'project folder', the current owner is gwappo@primetext.com
        
            [M174] Listing children of project folder (0B7oUW_j-nM7kfjFXWXd6cmdxbjltNGlfRHhnd3QyMmk2cnQwblNOSnJwdWpXdWZPNjBTMU0)...
            
                [M145] Processing child 'folder1', the current owner is roy.smith@primetext.com
                
                    [M149] !! changing owner permission to gwappo@primetext.com
                    
                    [M153] !! changing colour to #FF0000
                    
                    [M152] !! changing previous owner permission to writer
                    
                    [M174] Listing children of folder1 (0B7oUW_j-nM7kfjFXWXd6cmdxbjltNGlfRHhnd3QyMmk2cnQwblNOSnJwdWpXdWZPNjBTMU0)...
                    
                        [M145] Processing child 'file-in-folder1', the current owner is roy.smith@primetext.com
                        
                            [M149] !! changing owner permission to gwappo@primetext.com
                            


If you encounter any problems, please edit tof.cfg and set debug:true. Then run again and email me the log output.

## The gotchas

As we discussed on email, the above procedure changes ownership but leaves the original users with Write access. This allows them to delete files and folders. 
  The files will not be deleted, but they will be removed from their parent folder.
  
As I mentioned in one of my early emails, there is a bug in Drive that it is only possible to change the ownership of Google files. If you see
   
     [M154] Error: Could not add new owner permission com.google.api.client.googleapis.json.GoogleJsonResponseException: 400 Bad Request
     
     "message" : "Bad Request. User message: \"You can't yet change the owner of this item. (We're working on it.)\"",

then that's what happened. They've been working on it for years. This post on Google Plus refers https://plus.google.com/+RoySmith/posts/1LaTMRBtbsk.