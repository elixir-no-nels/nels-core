import re
import os
import smtplib
from email.MIMEMultipart import MIMEMultipart  
from email.MIMEBase import MIMEBase  
from email.MIMEText import MIMEText  
from email.Utils import COMMASPACE, formatdate  
from email import Encoders 

SMTP_MAIL_FROM = ""
SMTP_MAIL_SERVER = ""


def is_valid_email(email):
    return re.match("^.+\\@(\\[?)[a-zA-Z0-9\\-\\.]+\\.([a-zA-Z]{2,3}|[0-9]{1,3})(\\]?)$", email) != None

def send_mail(to, subject, text, frm=SMTP_MAIL_FROM, is_html=False,
              files=[], cc=[], bcc=[]): 
    assert type(to)==list  
    assert type(files)==list  
    assert type(cc)==list  
    assert type(bcc)==list  
 
    message = MIMEMultipart()  
    message['From'] = frm  
    message['To'] = COMMASPACE.join(to)  
    message['Date'] = formatdate(localtime=True)  
    message['Subject'] = subject  
    message['Cc'] = COMMASPACE.join(cc)  
    message.set_charset('UTF-8')

 
    if is_html:
        message.attach(MIMEText(text,'html'))
    else:
        message.attach(MIMEText(text,'plain'))
     
    for f in files:  
        part = MIMEBase('application', 'octet-stream')  
        part.set_payload(open(f, 'rb').read())  
        Encoders.encode_base64(part)  
        part.add_header('Content-Disposition', 'attachment; filename="%s"' % os.path.basename(f))  
        message.attach(part)  
 
    addresses = []  
    for x in to:  
        addresses.append(x)  
    for x in cc:  
        addresses.append(x)  
    for x in bcc:  
        addresses.append(x)  
 
    smtp = smtplib.SMTP(SMTP_MAIL_SERVER)  
    smtp.sendmail(frm, addresses, message.as_string())  
    smtp.close()