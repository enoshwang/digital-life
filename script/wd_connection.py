# -*- coding: utf-8 -*-

import paramiko      # ssh
import cx_Oracle     # oracle
import pymysql       # mysql
import asyncio

class WDConnection:
    def __init__(self,type:str='ssh'):
        self.type = type

    def __del__(self):
        #关闭连接
        pass

    def __repr__(self):
        return self.type

    def connect(self):
        # 建立连接
        pass

    def execute(self):
        # 执行
        pass

class WDMysql(WDConnection):
    def __init__(self,host:str = 'localhost',port:int = 3306,user:str = 'root',password:str = 'mysql',database:str = 'mysql'):
        super().__init__(type='mysql')
        self.host = host
        self.port = port
        self.user = user
        self.password = password
        self.database = database
        self.db = None

    def __del__(self):
        if self.db is not None:
            self.db.close()
        return super().__del__()
    
    def __repr__(self):
        return self.type

    def connect(self):
        # 打开数据库连接
        self.db = pymysql.connect(host=self.host,
                             port=self.port,
                             user=self.user,
                             password=self.password,
                             database=self.database)
        
    def execute(self,sql:str,type:str = 'select'):
        # 使用 cursor() 方法创建一个游标对象 cursor
        cursor = self.db.cursor()

        # 使用 execute()  方法执行 SQL 语句
        cursor.execute(sql)
        data = None
        if type == 'select':
            # 使用 fetchone() 方法获取单条数据.
            data = cursor.fetchone()
        elif type == 'selectall':
            # 使用 fetchall() 方法获取所有数据.
            data = cursor.fetchall()
        elif type == 'rowcount':
            # 使用 rowcount() 方法获取数据条数.
            data = cursor.rowcount
        elif type == 'insert':
            # 提交操作
            self.db.commit()
        elif type == 'update':
            # 提交操作
            self.db.commit()
        elif type == 'delete':
            # 提交操作
            self.db.commit()
        else :
            pass

        return data


class WDSsh(WDConnection):
    def __init__(self,host:str='localhost',port:int=22,user:str='root',password:str='root'):
        super().__init__(type='ssh')
        self.host = host
        self.port = port
        self.user = user
        self.password = password
        self.ssh = None

    def __del__(self):
        if self.ssh is not None:
            self.ssh.close()
        return super().__del__()
    
    def __repr__(self):
        return self.type

    def connect(self):
        self.ssh = paramiko.SSHClient()
        self.ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
        # 建立连接
        self.ssh.connect(self.host,self.port,self.user,self.password,timeout = 30)
        
    def execute(self,cmd:str):
        stdin,stdout,stderr = self.ssh.exec_command(cmd)
        result = stdout.read()
        return result
    
class WDOracle(WDConnection):
    def __init__(self,user:str = 'root',password:str = 'mysql',url:str = 'localhost:1521/orcl'):
        super().__init__(type='mysql')
        self.user = user
        self.password = password
        self.url = url
        self.db = None

    def __del__(self):
        if self.db is not None:
            self.db.close()
        return super().__del__()
    
    def __repr__(self):
        return self.type

    def connect(self):
        db = cx_Oracle.connect(self.user, self.password,self.url)
        
    def execute(self,sql:str,type:str = 'select'):
        # 使用 cursor() 方法创建一个游标对象 cursor
        cursor = self.db.cursor()

        ''' 执行 sql 语句方法 1
        sql = \'''
            SELECT *
            FROM user
            WHERE USER_ID = :did
            \'''
        cr.execute(sql,did = '110')
        '''
        # 使用 execute()  方法执行 SQL 语句
        cursor.execute(sql)
        data = None
        if type == 'select':
            # 使用 fetchone() 方法获取单条数据.
            data = cursor.fetchone()
        elif type == 'selectall':
            # 使用 fetchall() 方法获取所有数据.
            data = cursor.fetchall()
        elif type == 'rowcount':
            # 使用 rowcount() 方法获取数据条数.
            data = cursor.rowcount
        elif type == 'insert':
            # 提交操作
            self.db.commit()
        elif type == 'update':
            # 提交操作
            self.db.commit()
        elif type == 'delete':
            # 提交操作
            self.db.commit()
        else :
            pass

        return data

async def run(q: asyncio.Queue):
    while True:
        asyncio.sleep(1)

async def main():
    q = asyncio.Queue()
    L = await asyncio.gather(
        run(q)
    )
    print(L)

if __name__ == '__main__':
    wd_mysql : WDMysql = WDMysql(host='',password='',database='')
    wd_mysql.connect()
    print(wd_mysql.execute('select * from User',type='selectall'))
    asyncio.run(main())

