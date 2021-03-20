
drop table memberofcourse;
drop table threadinfolder;
drop table hasparentfolder;
drop table folder;
drop table courseInYear;
drop table course;
drop table replyto;
drop table likespost;
drop table viewedpost;
drop table post;
drop table user;



create table user (
email			varchar(30),
userPassword		varchar(30) not null,
userName		varchar(30) not null,
constraint user_pk primary key (email) );

create table course(
courseID	char(7),
courseName	varchar(30),
term		varchar(30) not null,
constraint course_pk primary key (courseID) );
    
create table courseInYear(
courseID		char(7),
courseYear		integer,
allowAnonumous	boolean not null,
constraint cIY_pk1 primary key (courseID, courseYear),
constraint cIY_fk foreign key (courseID) references course(courseID)
	on update cascade 
    	on delete cascade );

create table memberOfCourse(
email		varchar(30),
courseID	char(7),
mocYear	integer,
isInstructor	boolean not null,
constraint mOC_pk1 primary key (email, courseID, mocYear),
constraint mOC_fk1 foreign key (email) references user(email)
	on update cascade
    	on delete cascade,
constraint mOC_fk2 foreign key (courseID, mocYear) references courseInYear(courseID, 
courseYear)
	on update cascade
    	on delete cascade );




create table folder(
	courseID	char(7),
	folderYear	integer,
	folderName	varchar(30),
    	constraint folder_pk1 primary key (courseID, folderYear, folderName),
    	constraint folder_fk1 foreign key (courseID, folderYear) references 
courseInYear(courseID, courseYear)
		on update cascade 
        		on delete cascade );

create table hasParentFolder(
	parentCourseID	char(7),
	parentYear		integer,
	parentName 		varchar(30),
	subCourseID 		char(7),
	subYear 		integer,
	subName		varchar(30),
    	constraint hPF_pk1 primary key (parentCourseID, parentYear, parentName, 
subCourseID, subYear, subName), 
    	constraint hPF_fk1 foreign key (parentCourseID, parentYear, parentName) 
references folder(courseID, folderYear, folderName)
		on update cascade 
    		on delete cascade,
    	constraint hPF_fk2 foreign key (subCourseID, subYear, subName) references 
folder(courseID, folderYear, folderName)
		on update cascade 
    		on delete cascade );
    
create table post(
	postID			integer, 
   	threadID		integer not null,
	userEmail		varchar(30) not null,
	postName		varchar(30) not null, 
	content		varchar(500) not null, 
	tag			varchar(30),
	isAnonymous		boolean not null, 
    	creationTime		time not null,
    	constraint post_pk primary key (postID),
	constraint post_fk1 foreign key (userEmail) references user(email)
    		on update cascade 
		on delete cascade,
	constraint post_fk2 foreign key (threadID) references post(postID)
		on update cascade 
		on delete cascade );




create table threadInFolder(
postID				integer,
folderName			varchar(30) not null,
folderCourseID		char(7) not null,
folderCourseYear		integer not null,
constraint tIF_pk primary key (postID), 
constraint tIF_fk1 foreign key (postID) references post(postID)
		on update cascade 
		on delete cascade,
constraint tIF_fk2 foreign key (folderCourseID, folderCourseYear, folderName) 
references folder(courseID, folderYear, folderName)
		on update cascade 
		on delete cascade );

create table replyTo(
postID		integer,
parentID	integer not null,
constraint rT_pk primary key (postID),
constraint rT_fk1 foreign key (postID) references post(postID)
		on update cascade 
    		on delete cascade, 
constraint rT_fk2 foreign key (parentID) references post(postID)
		on update cascade 
   		on delete cascade );

create table likesPost(
email	varchar(30),
postID	integer,
constraint lP_pk1 primary key (email, postID),
constraint lP_fk1 foreign key (email) references user(email)
		on update cascade 
   		 on delete cascade,
constraint lP_fk2 foreign key (postID) references post(postID)
		on update cascade
    		on delete cascade );


create table viewedPost(
email		varchar(30),
postID		integer,
vpYear		integer not null,
vpMonth	integer not null,
voDay		integer not null,
constraint vP_pk1 primary key (email, postID),
constraint vP_fk1 foreign key (email) references user(email)
	on update cascade 
    	on delete cascade,
constraint vP_fk2 foreign key (postID) references post(postID)
	on update cascade
   	on delete cascade );

 insert into user values("bendik@gmail.com", "bendik", "bendik");
 insert into course values("TDT4100", "Objektorientert programmering", "vår");
 insert into courseInYear values("TDT4100", 2021, true); 
 insert into memberOfCourse values("bendik@gmail.com", "TDT4100", 2021, false);
 insert into folder values("TDT4100", 2021, "eksamen");
 
 insert into user values("jarl@gmail.com", "jarl", "jarl");
 insert into memberOfCourse values("jarl@gmail.com", "TDT4100", 2021, true);
 

