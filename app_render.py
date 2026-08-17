# -*- coding: utf-8 -*-
"""
PluxChest 许可证系统 - Render 部署版本
支持 PostgreSQL，所有敏感配置通过环境变量读取
"""
import os
import sys
import sqlite3
import hashlib
import random
import string
import uuid
import time
import requests
import stripe
from datetime import datetime, timedelta
from functools import wraps
from io import BytesIO

from flask import Flask, request, jsonify, render_template, redirect, url_for, flash, session, send_file
from flask_cors import CORS
from flask_login import LoginManager, UserMixin, login_user, login_required, logout_user, current_user
from flask_mail import Mail, Message
from flask_sqlalchemy import SQLAlchemy
from captcha.image import ImageCaptcha
from dotenv import load_dotenv

# 加载 .env 文件（本地测试用）
load_dotenv()

app = Flask(__name__)

# ---------- 基础配置 ----------
app.secret_key = os.environ.get('SECRET_KEY', 'f714f7aa2107221d2d817691b563127474caebad10beea9f')
CORS(app)

# ---------- 数据库配置 ----------
# Render 会自动设置 RENDER 环境变量
IS_PRODUCTION = os.environ.get('RENDER') == 'true'

if IS_PRODUCTION:
    # 生产环境：使用 PostgreSQL
    database_url = os.environ.get('DATABASE_URL')
    if not database_url:
        raise ValueError("DATABASE_URL environment variable is not set")
    # Render 的 DATABASE_URL 是 postgres:// 格式，SQLAlchemy 需要 postgresql://
    if database_url.startswith('postgres://'):
        database_url = database_url.replace('postgres://', 'postgresql://', 1)
    app.config['SQLALCHEMY_DATABASE_URI'] = database_url
    app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
    db = SQLAlchemy(app)
    print("✅ 使用 PostgreSQL 数据库 (Render)")
else:
    # 本地开发：使用 SQLite（兼容原有逻辑）
    DB_PATH = 'licenses.db'
    print("✅ 使用 SQLite 数据库 (本地开发)")

# ---------- 支付配置（epay.ffomu.net）----------
# 优先从环境变量读取，否则使用硬编码（你提供的）
EPAY_API_URL = os.environ.get('EPAY_API_URL', 'https://epay.ffomu.net/api/')
EPAY_PID = os.environ.get('EPAY_PID', 'MB473E0894EEDC4C2')
EPAY_KEY = os.environ.get('EPAY_KEY', '5eee0cc91d2c46f75222ef8e209a0c992be7674fcd5f301a63a2af9cc282c088')

# 回调地址（必须在 Render 环境变量中设置，或使用默认占位）
NOTIFY_URL = os.environ.get('NOTIFY_URL', 'https://你的服务名.onrender.com/pay_notify')
RETURN_URL = os.environ.get('RETURN_URL', 'https://你的服务名.onrender.com/pay_return')

# ---------- Stripe（备用，可不填）----------
stripe.api_key = os.environ.get('STRIPE_SECRET_KEY', 'sk_test_xxx')
STRIPE_PUBLISHABLE_KEY = os.environ.get('STRIPE_PUBLISHABLE_KEY', 'pk_test_xxx')

# ---------- 邮件配置 ----------
app.config['MAIL_SERVER'] = os.environ.get('MAIL_SERVER', 'smtp.qq.com')
app.config['MAIL_PORT'] = int(os.environ.get('MAIL_PORT', 587))
app.config['MAIL_USE_TLS'] = os.environ.get('MAIL_USE_TLS', 'true').lower() == 'true'
app.config['MAIL_USERNAME'] = os.environ.get('MAIL_USERNAME', '3607483986@qq.com')
app.config['MAIL_PASSWORD'] = os.environ.get('MAIL_PASSWORD', 'mmdohsgiinezcihf')
app.config['MAIL_DEFAULT_SENDER'] = os.environ.get('MAIL_DEFAULT_SENDER', app.config['MAIL_USERNAME'])
mail = Mail(app)

# ---------- 验证码存储 ----------
captcha_store = {}

# ---------- LoginManager ----------
login_manager = LoginManager()
login_manager.init_app(app)
login_manager.login_view = 'login'

# ============================================================
# 数据库模型（仅 PostgreSQL 使用）
# ============================================================
if IS_PRODUCTION:
    # 定义模型
    class User(UserMixin, db.Model):
        __tablename__ = 'users'
        id = db.Column(db.Integer, primary_key=True)
        username = db.Column(db.String(80), unique=True, nullable=False)
        password = db.Column(db.String(255), nullable=False)
        email = db.Column(db.String(120), unique=True, nullable=True)
        created_at = db.Column(db.String(50))

    class Product(db.Model):
        __tablename__ = 'products'
        id = db.Column(db.Integer, primary_key=True)
        name = db.Column(db.String(200), nullable=False)
        description = db.Column(db.Text)
        price = db.Column(db.Float, nullable=False)
        currency = db.Column(db.String(10), default='cny')
        is_active = db.Column(db.Integer, default=1)
        created_at = db.Column(db.String(50))

    class License(db.Model):
        __tablename__ = 'licenses'
        license_key = db.Column(db.String(50), primary_key=True)
        user_id = db.Column(db.Integer, db.ForeignKey('users.id'))
        product_id = db.Column(db.Integer, db.ForeignKey('products.id'))
        expiry_date = db.Column(db.String(20))
        max_servers = db.Column(db.Integer, default=1)
        is_active = db.Column(db.Integer, default=1)
        created_at = db.Column(db.String(50))

    class Order(db.Model):
        __tablename__ = 'orders'
        id = db.Column(db.Integer, primary_key=True)
        user_id = db.Column(db.Integer, db.ForeignKey('users.id'))
        product_id = db.Column(db.Integer, db.ForeignKey('products.id'))
        out_trade_no = db.Column(db.String(50), unique=True)
        trade_no = db.Column(db.String(50))
        amount = db.Column(db.Float)
        currency = db.Column(db.String(10))
        payment_type = db.Column(db.String(20))
        payment_id = db.Column(db.String(100))
        payment_status = db.Column(db.String(20))
        license_key = db.Column(db.String(50))
        created_at = db.Column(db.String(50))

    class Purchase(db.Model):
        __tablename__ = 'purchases'
        id = db.Column(db.Integer, primary_key=True)
        user_id = db.Column(db.Integer, db.ForeignKey('users.id'))
        purchase_token = db.Column(db.String(100), unique=True)
        verified = db.Column(db.Integer, default=0)
        created_at = db.Column(db.String(50))

    class Activation(db.Model):
        __tablename__ = 'activations'
        id = db.Column(db.Integer, primary_key=True)
        license_key = db.Column(db.String(50), db.ForeignKey('licenses.license_key'))
        server_hwid = db.Column(db.String(200))
        server_ip = db.Column(db.String(50))
        activated_at = db.Column(db.String(50))
        last_verify = db.Column(db.String(50))

    # 创建所有表
    with app.app_context():
        db.create_all()
        print("✅ PostgreSQL 表创建成功")

    # ----- PostgreSQL 版工具函数 -----
    def get_activation_count(license_key):
        return Activation.query.filter_by(license_key=license_key).distinct(Activation.server_hwid).count()

    def is_server_activated(license_key, hwid):
        return Activation.query.filter_by(license_key=license_key, server_hwid=hwid).first() is not None

    def get_license_info(license_key):
        lic = License.query.filter_by(license_key=license_key).first()
        if lic:
            return (lic.expiry_date, lic.max_servers, lic.is_active)
        return None

    # 初始化默认管理员和产品
    def init_db():
        if User.query.filter_by(username='admin').first() is None:
            admin_pwd = hashlib.sha256('admin123'.encode()).hexdigest()
            admin = User(username='admin', password=admin_pwd, created_at=datetime.now().strftime('%Y-%m-%d %H:%M:%S'))
            db.session.add(admin)
            db.session.commit()
            print("✅ 默认管理员已创建 (admin/admin123)")

        if Product.query.count() == 0:
            products = [
                Product(name='PluxChest 标准版', description='高性能箱子管理插件，支持自定义菜单、权限联动。', price=99.00, currency='cny', created_at=datetime.now().strftime('%Y-%m-%d %H:%M:%S')),
                Product(name='PluxChest 专业版', description='包含全部功能，额外支持经济系统、数据统计。', price=199.00, currency='cny', created_at=datetime.now().strftime('%Y-%m-%d %H:%M:%S'))
            ]
            db.session.add_all(products)
            db.session.commit()
            print("✅ 示例产品已创建")

    init_db()

else:
    # ----- SQLite 版（本地开发）-----
    def init_db_sqlite():
        conn = sqlite3.connect(DB_PATH)
        c = conn.cursor()
        c.execute('''
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT UNIQUE NOT NULL,
                password TEXT NOT NULL,
                email TEXT UNIQUE,
                created_at TEXT
            )
        ''')
        c.execute('''
            CREATE TABLE IF NOT EXISTS products (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                description TEXT,
                price REAL NOT NULL,
                currency TEXT DEFAULT 'cny',
                is_active INTEGER DEFAULT 1,
                created_at TEXT
            )
        ''')
        c.execute('''
            CREATE TABLE IF NOT EXISTS licenses (
                license_key TEXT PRIMARY KEY,
                user_id INTEGER,
                product_id INTEGER,
                expiry_date TEXT,
                max_servers INTEGER DEFAULT 1,
                is_active INTEGER DEFAULT 1,
                created_at TEXT,
                FOREIGN KEY (user_id) REFERENCES users(id),
                FOREIGN KEY (product_id) REFERENCES products(id)
            )
        ''')
        c.execute('''
            CREATE TABLE IF NOT EXISTS orders (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER,
                product_id INTEGER,
                out_trade_no TEXT UNIQUE,
                trade_no TEXT,
                amount REAL,
                currency TEXT,
                payment_type TEXT,
                payment_id TEXT,
                payment_status TEXT,
                license_key TEXT,
                created_at TEXT,
                FOREIGN KEY (user_id) REFERENCES users(id),
                FOREIGN KEY (product_id) REFERENCES products(id)
            )
        ''')
        c.execute('''
            CREATE TABLE IF NOT EXISTS purchases (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER,
                purchase_token TEXT UNIQUE,
                verified INTEGER DEFAULT 0,
                created_at TEXT,
                FOREIGN KEY (user_id) REFERENCES users(id)
            )
        ''')
        c.execute('''
            CREATE TABLE IF NOT EXISTS activations (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                license_key TEXT,
                server_hwid TEXT,
                server_ip TEXT,
                activated_at TEXT,
                last_verify TEXT,
                FOREIGN KEY (license_key) REFERENCES licenses(license_key)
            )
        ''')
        c.execute('''
            CREATE TABLE IF NOT EXISTS verify_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                license_key TEXT,
                server_ip TEXT,
                server_hwid TEXT,
                result TEXT,
                reason TEXT,
                created_at TEXT
            )
        ''')
        admin_pwd = hashlib.sha256('admin123'.encode()).hexdigest()
        c.execute('INSERT OR IGNORE INTO users (id, username, password, created_at) VALUES (1, ?, ?, ?)',
                  ('admin', admin_pwd, datetime.now().strftime('%Y-%m-%d %H:%M:%S')))
        c.execute('SELECT COUNT(*) FROM products')
        if c.fetchone()[0] == 0:
            c.execute('INSERT INTO products (name, description, price, currency, created_at) VALUES (?, ?, ?, ?, ?)',
                      ('PluxChest 标准版', '高性能箱子管理插件，支持自定义菜单、权限联动。', 99.00, 'cny', datetime.now().strftime('%Y-%m-%d %H:%M:%S')))
            c.execute('INSERT INTO products (name, description, price, currency, created_at) VALUES (?, ?, ?, ?, ?)',
                      ('PluxChest 专业版', '包含全部功能，额外支持经济系统、数据统计。', 199.00, 'cny', datetime.now().strftime('%Y-%m-%d %H:%M:%S')))
        conn.commit()
        conn.close()
    init_db_sqlite()

    # ----- SQLite 版工具函数 -----
    def get_activation_count(license_key):
        conn = sqlite3.connect(DB_PATH)
        c = conn.cursor()
        c.execute('SELECT COUNT(DISTINCT server_hwid) FROM activations WHERE license_key = ?', (license_key,))
        count = c.fetchone()[0]
        conn.close()
        return count

    def is_server_activated(license_key, hwid):
        conn = sqlite3.connect(DB_PATH)
        c = conn.cursor()
        c.execute('SELECT 1 FROM activations WHERE license_key = ? AND server_hwid = ?', (license_key, hwid))
        exists = c.fetchone()
        conn.close()
        return exists is not None

    def get_license_info(license_key):
        conn = sqlite3.connect(DB_PATH)
        c = conn.cursor()
        c.execute('SELECT expiry_date, max_servers, is_active FROM licenses WHERE license_key = ?', (license_key,))
        row = c.fetchone()
        conn.close()
        return row

# ============================================================
# 通用工具函数
# ============================================================
def generate_order_no():
    return f"PLUX{int(time.time())}{random.randint(1000, 9999)}"

def admin_required(f):
    @wraps(f)
    def decorated(*args, **kwargs):
        if not current_user.is_authenticated or current_user.username != 'admin':
            flash('需要管理员权限！')
            return redirect(url_for('index'))
        return f(*args, **kwargs)
    return decorated

# ---------- Flask-Login 用户加载 ----------
@login_manager.user_loader
def load_user(user_id):
    if IS_PRODUCTION:
        return User.query.get(int(user_id))
    else:
        conn = sqlite3.connect(DB_PATH)
        c = conn.cursor()
        c.execute('SELECT id, username, password, email FROM users WHERE id = ?', (user_id,))
        user = c.fetchone()
        conn.close()
        if user:
            # 构造一个简单的 User 对象（因 SQLite 没有模型）
            class SimpleUser(UserMixin):
                def __init__(self, id, username, password, email):
                    self.id = id
                    self.username = username
                    self.password = password
                    self.email = email
            return SimpleUser(user[0], user[1], user[2], user[3])
        return None

# ============================================================
# API：插件验证接口
# ============================================================
@app.route('/api/verify', methods=['GET'])
def verify_license():
    license_key = request.args.get('key')
    hwid = request.args.get('hwid', 'unknown')
    server_ip = request.remote_addr

    print(f"📥 验证请求: key={license_key}, hwid={hwid}")

    if not license_key:
        return jsonify({'valid': False, 'reason': 'Missing license key'})

    info = get_license_info(license_key)
    if not info:
        return jsonify({'valid': False, 'reason': 'Invalid license key'})

    expiry_date, max_servers, is_active = info

    if not is_active:
        return jsonify({'valid': False, 'reason': 'License is disabled'})

    today = datetime.now().strftime('%Y-%m-%d')
    if today > expiry_date:
        return jsonify({'valid': False, 'reason': 'License expired', 'expiry': expiry_date})

    current_count = get_activation_count(license_key)
    if current_count >= max_servers:
        if not is_server_activated(license_key, hwid):
            return jsonify({'valid': False, 'reason': 'Too many active servers', 'limit': max_servers})

    # 记录激活
    now = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
    if IS_PRODUCTION:
        activation = Activation.query.filter_by(license_key=license_key, server_hwid=hwid).first()
        if activation:
            activation.last_verify = now
        else:
            activation = Activation(license_key=license_key, server_hwid=hwid, server_ip=server_ip, activated_at=now, last_verify=now)
            db.session.add(activation)
        db.session.commit()
    else:
        conn = sqlite3.connect(DB_PATH)
        c = conn.cursor()
        if is_server_activated(license_key, hwid):
            c.execute('UPDATE activations SET last_verify = ? WHERE license_key = ? AND server_hwid = ?',
                      (now, license_key, hwid))
        else:
            c.execute('INSERT INTO activations (license_key, server_hwid, server_ip, activated_at, last_verify) VALUES (?, ?, ?, ?, ?)',
                      (license_key, hwid, server_ip, now, now))
        conn.commit()
        conn.close()

    return jsonify({'valid': True, 'expiry': expiry_date, 'max_servers': max_servers})

# ============================================================
# 健康检查
# ============================================================
@app.route('/health')
def health_check():
    return jsonify({'status': 'healthy', 'timestamp': datetime.now().isoformat()})

# ============================================================
# 用户官网路由
# ============================================================

# ---------- 首页 ----------
@app.route('/')
def index():
    purchase_token = session.get('purchase_token')
    if purchase_token:
        if IS_PRODUCTION:
            purchase = Purchase.query.filter_by(purchase_token=purchase_token).first()
            if purchase and purchase.verified == 0:
                return redirect(url_for('verify_purchase'))
        else:
            conn = sqlite3.connect(DB_PATH)
            c = conn.cursor()
            c.execute('SELECT verified FROM purchases WHERE purchase_token = ?', (purchase_token,))
            row = c.fetchone()
            conn.close()
            if row and row[0] == 0:
                return redirect(url_for('verify_purchase'))

    if IS_PRODUCTION:
        products = Product.query.filter_by(is_active=1).all()
    else:
        conn = sqlite3.connect(DB_PATH)
        c = conn.cursor()
        c.execute('SELECT id, name, description, price, currency FROM products WHERE is_active = 1 ORDER BY id')
        products = c.fetchall()
        conn.close()
    return render_template('index.html', products=products)

# ---------- 图形验证码 ----------
@app.route('/captcha')
def captcha():
    image = ImageCaptcha()
    code = ''.join(random.choices(string.ascii_uppercase + string.digits, k=6))
    captcha_store[request.remote_addr] = code
    data = image.generate(code)
    return send_file(BytesIO(data.getvalue()), mimetype='image/png')

# ---------- 发送邮箱验证码 ----------
@app.route('/send-email-code', methods=['POST'])
def send_email_code():
    try:
        if not request.is_json:
            return jsonify({'error': '请求格式错误，需要 JSON'}), 400

        email = request.json.get('email')
        if not email or '@' not in email:
            return jsonify({'error': '邮箱格式不正确'}), 400

        code = ''.join(random.choices(string.digits, k=6))
        session['email_code'] = code
        now = datetime.now().strftime('%Y-%m-%d %H:%M:%S')

        msg = Message('注册验证码', recipients=[email])
        msg.body = f'尊敬的用户，您好：\n\n您正在申请操作验证，本次验证码为：{code} ，有效期五分钟\n发送时间：{now}\n如非您本人操作，请及时联系客服\nQQ群 286219229'
        msg.charset = 'utf-8'
        mail.send(msg)
        print(f"✅ 验证码 {code} 已发送到 {email}")
        return jsonify({'success': True})
    except Exception as e:
        import traceback
        traceback.print_exc()
        return jsonify({'error': f'服务器错误：{str(e)}'}), 500

# ---------- 注册 ----------
@app.route('/register', methods=['GET', 'POST'])
def register():
    if request.method == 'POST':
        username = request.form['username'].strip()
        password = request.form['password']
        email = request.form['email'].strip()
        captcha_input = request.form.get('captcha', '')
        email_code_input = request.form.get('email_code', '')

        if captcha_store.get(request.remote_addr, '').upper() != captcha_input.upper():
            flash('图形验证码错误')
            return render_template('register.html')

        if session.get('email_code') != email_code_input:
            flash('邮箱验证码错误')
            return render_template('register.html')

        # 检查用户是否存在
        if IS_PRODUCTION:
            existing = User.query.filter((User.username == username) | (User.email == email)).first()
            if existing:
                flash('用户名或邮箱已被占用')
                return render_template('register.html')
        else:
            conn = sqlite3.connect(DB_PATH)
            c = conn.cursor()
            c.execute('SELECT id FROM users WHERE username = ? OR email = ?', (username, email))
            if c.fetchone():
                flash('用户名或邮箱已被占用')
                conn.close()
                return render_template('register.html')
            conn.close()

        hashed_pwd = hashlib.sha256(password.encode()).hexdigest()
        now = datetime.now().strftime('%Y-%m-%d %H:%M:%S')

        if IS_PRODUCTION:
            user = User(username=username, password=hashed_pwd, email=email, created_at=now)
            db.session.add(user)
            db.session.commit()
        else:
            conn = sqlite3.connect(DB_PATH)
            c = conn.cursor()
            c.execute('INSERT INTO users (username, password, email, created_at) VALUES (?, ?, ?, ?)',
                      (username, hashed_pwd, email, now))
            conn.commit()
            conn.close()

        flash('注册成功！请登录')
        return redirect(url_for('login'))
    return render_template('register.html')

# ---------- 登录 ----------
@app.route('/login', methods=['GET', 'POST'])
def login():
    if request.method == 'POST':
        login_input = request.form['username'].strip()
        password = request.form['password']
        hashed_pwd = hashlib.sha256(password.encode()).hexdigest()

        if IS_PRODUCTION:
            user = User.query.filter((User.username == login_input) | (User.email == login_input)).first()
            if user and user.password == hashed_pwd:
                login_user(UserMixin())  # 需要包装
                # 由于我们没有真正的 User 对象，我们可以用 session 存储简单信息
                session['user_id'] = user.id
                session['username'] = user.username
                return redirect(url_for('dashboard'))
            else:
                flash('用户名/邮箱或密码错误')
        else:
            conn = sqlite3.connect(DB_PATH)
            c = conn.cursor()
            c.execute('SELECT id, username, password, email FROM users WHERE username = ? OR email = ?', (login_input, login_input))
            user = c.fetchone()
            conn.close()
            if user and user[2] == hashed_pwd:
                # 使用 login_user 需要 User 对象，我们构建一个简单对象
                class SimpleUser(UserMixin):
                    def __init__(self, id, username, password, email):
                        self.id = id
                        self.username = username
                        self.password = password
                        self.email = email
                login_user(SimpleUser(user[0], user[1], user[2], user[3]))
                return redirect(url_for('dashboard'))
            else:
                flash('用户名/邮箱或密码错误')
    return render_template('login.html')

@app.route('/logout')
@login_required
def logout():
    logout_user()
    session.pop('purchase_token', None)
    return redirect(url_for('index'))

# ---------- 用户面板 ----------
@app.route('/dashboard')
@login_required
def dashboard():
    user_id = current_user.id
    if IS_PRODUCTION:
        licenses = db.session.query(License, Product.name).join(Product, License.product_id == Product.id).filter(License.user_id == user_id).all()
        # 转换格式
        licenses = [(l.License.license_key, l.name, l.License.expiry_date, l.License.max_servers, l.License.is_active, l.License.created_at) for l in licenses]
    else:
        conn = sqlite3.connect(DB_PATH)
        c = conn.cursor()
        c.execute('''
            SELECT l.license_key, p.name, l.expiry_date, l.max_servers, l.is_active, l.created_at
            FROM licenses l
            JOIN products p ON l.product_id = p.id
            WHERE l.user_id = ?
            ORDER BY l.created_at DESC
        ''', (user_id,))
        licenses = c.fetchall()
        conn.close()
    return render_template('dashboard.html', licenses=licenses)

# ---------- 购买页面 ----------
@app.route('/buy/<int:product_id>')
@login_required
def buy(product_id):
    if IS_PRODUCTION:
        product = Product.query.filter_by(id=product_id, is_active=1).first()
        if not product:
            flash('产品不存在或已下架')
            return redirect(url_for('index'))
        product = (product.id, product.name, product.description, product.price, product.currency)
    else:
        conn = sqlite3.connect(DB_PATH)
        c = conn.cursor()
        c.execute('SELECT id, name, description, price, currency FROM products WHERE id = ? AND is_active = 1', (product_id,))
        product = c.fetchone()
        conn.close()
        if not product:
            flash('产品不存在或已下架')
            return redirect(url_for('index'))
    return render_template('buy.html', product=product)

# ---------- 创建支付 ----------
@app.route('/create-payment', methods=['POST'])
@login_required
def create_payment():
    data = request.get_json()
    product_id = data.get('product_id')
    if not product_id:
        return jsonify({'error': '缺少产品ID'}), 400

    if IS_PRODUCTION:
        product = Product.query.filter_by(id=product_id, is_active=1).first()
        if not product:
            return jsonify({'error': '产品无效'}), 400
        name, price, currency = product.name, product.price, product.currency
    else:
        conn = sqlite3.connect(DB_PATH)
        c = conn.cursor()
        c.execute('SELECT name, price, currency FROM products WHERE id = ? AND is_active = 1', (product_id,))
        product = c.fetchone()
        conn.close()
        if not product:
            return jsonify({'error': '产品无效'}), 400
        name, price, currency = product

    out_trade_no = generate_order_no()
    total_amount = str(price)
    pay_type = data.get('pay_type', 'alipay')

    params = {
        'pid': EPAY_PID,
        'type': pay_type,
        'out_trade_no': out_trade_no,
        'notify_url': NOTIFY_URL,
        'return_url': RETURN_URL,
        'name': name,
        'money': total_amount,
        'sitename': 'PluxChest'
    }
    # 签名
    sign_str = '&'.join([f'{k}={params[k]}' for k in sorted(params)]) + '&key=' + EPAY_KEY
    params['sign'] = hashlib.md5(sign_str.encode('utf-8')).hexdigest()
    params['sign_type'] = 'MD5'

    try:
        resp = requests.post(EPAY_API_URL + 'submit', data=params, timeout=10)
        result = resp.json()
        if result.get('code') == 0:
            pay_url = result.get('qrcode') or result.get('payurl')
            # 存入订单
            now = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
            if IS_PRODUCTION:
                order = Order(
                    user_id=current_user.id,
                    product_id=product_id,
                    out_trade_no=out_trade_no,
                    amount=price,
                    currency=currency,
                    payment_type=pay_type,
                    payment_status='pending',
                    created_at=now
                )
                db.session.add(order)
                db.session.commit()
            else:
                conn = sqlite3.connect(DB_PATH)
                c = conn.cursor()
                c.execute('''
                    INSERT INTO orders (user_id, product_id, out_trade_no, amount, currency, payment_type, payment_status, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ''', (current_user.id, product_id, out_trade_no, price, currency, pay_type, 'pending', now))
                conn.commit()
                conn.close()
            return jsonify({'pay_url': pay_url, 'out_trade_no': out_trade_no})
        else:
            return jsonify({'error': result.get('msg', '支付平台返回错误')}), 400
    except Exception as e:
        return jsonify({'error': f'请求支付平台失败：{str(e)}'}), 500

# ---------- 支付异步通知 ----------
@app.route('/pay_notify', methods=['POST'])
def pay_notify():
    data = request.form.to_dict()
    if not data:
        return '参数错误', 400

    # 验证签名
    sign = data.pop('sign', '')
    sign_str = '&'.join([f'{k}={data[k]}' for k in sorted(data)]) + '&key=' + EPAY_KEY
    if hashlib.md5(sign_str.encode('utf-8')).hexdigest() != sign:
        return '签名验证失败', 400

    out_trade_no = data.get('out_trade_no')
    trade_no = data.get('trade_no')
    money = data.get('money')
    status = data.get('status')
    if status != '1':
        return '支付未完成', 200

    if not out_trade_no:
        return '订单号缺失', 400

    # 更新订单
    if IS_PRODUCTION:
        order = Order.query.filter_by(out_trade_no=out_trade_no, payment_status='pending').first()
        if not order:
            return '订单不存在或已处理', 200
        # 生成许可证
        license_key = 'PLUX-' + hashlib.md5(os.urandom(16)).hexdigest().upper()[:12]
        now = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        expiry = (datetime.now() + timedelta(days=365)).strftime('%Y-%m-%d')
        lic = License(
            license_key=license_key,
            user_id=order.user_id,
            product_id=order.product_id,
            expiry_date=expiry,
            max_servers=1,
            created_at=now
        )
        db.session.add(lic)
        order.payment_status = 'paid'
        order.trade_no = trade_no
        order.license_key = license_key
        order.payment_id = trade_no
        # 购买记录
        purchase_token = hashlib.sha256(os.urandom(32)).hexdigest()
        purchase = Purchase(user_id=order.user_id, purchase_token=purchase_token, verified=0, created_at=now)
        db.session.add(purchase)
        db.session.commit()
        print(f"✅ 订单 {out_trade_no} 支付成功，已发放许可证 {license_key}")
    else:
        conn = sqlite3.connect(DB_PATH)
        c = conn.cursor()
        c.execute('SELECT id, user_id, product_id FROM orders WHERE out_trade_no = ? AND payment_status = ?', (out_trade_no, 'pending'))
        order = c.fetchone()
        if not order:
            conn.close()
            return '订单不存在或已处理', 200
        order_id, user_id, product_id = order
        license_key = 'PLUX-' + hashlib.md5(os.urandom(16)).hexdigest().upper()[:12]
        now = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        expiry = (datetime.now() + timedelta(days=365)).strftime('%Y-%m-%d')
        c.execute('''
            INSERT INTO licenses (license_key, user_id, product_id, expiry_date, max_servers, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
        ''', (license_key, user_id, product_id, expiry, 1, now))
        c.execute('''
            UPDATE orders SET trade_no=?, payment_status='paid', license_key=?, payment_id=?
            WHERE out_trade_no=?
        ''', (trade_no, license_key, trade_no, out_trade_no))
        purchase_token = hashlib.sha256(os.urandom(32)).hexdigest()
        c.execute('''
            INSERT INTO purchases (user_id, purchase_token, verified, created_at)
            VALUES (?, ?, ?, ?)
        ''', (user_id, purchase_token, 0, now))
        conn.commit()
        conn.close()
        print(f"✅ 订单 {out_trade_no} 支付成功，已发放许可证 {license_key}")

    return 'success'

# ---------- 支付同步跳转 ----------
@app.route('/pay_return')
def pay_return():
    flash('支付已提交，请等待系统确认（通常几秒内完成）。')
    return redirect(url_for('dashboard'))

# ---------- 二次验证 ----------
@app.route('/verify-purchase', methods=['GET', 'POST'])
def verify_purchase():
    purchase_token = session.get('purchase_token')
    if not purchase_token:
        return redirect(url_for('index'))

    if IS_PRODUCTION:
        purchase = Purchase.query.filter_by(purchase_token=purchase_token).first()
        if not purchase:
            session.pop('purchase_token', None)
            return redirect(url_for('index'))
        user_id = purchase.user_id
        verified = purchase.verified
        if verified:
            return redirect(url_for('dashboard'))
        if request.method == 'POST':
            password = request.form.get('password')
            user = User.query.get(user_id)
            if user and hashlib.sha256(password.encode()).hexdigest() == user.password:
                purchase.verified = 1
                db.session.commit()
                flash('验证成功！')
                return redirect(url_for('dashboard'))
            else:
                flash('密码错误，请重试')
    else:
        conn = sqlite3.connect(DB_PATH)
        c = conn.cursor()
        c.execute('SELECT user_id, verified FROM purchases WHERE purchase_token = ?', (purchase_token,))
        row = c.fetchone()
        conn.close()
        if not row:
            session.pop('purchase_token', None)
            return redirect(url_for('index'))
        user_id, verified = row
        if verified:
            return redirect(url_for('dashboard'))
        if request.method == 'POST':
            password = request.form.get('password')
            conn = sqlite3.connect(DB_PATH)
            c = conn.cursor()
            c.execute('SELECT password FROM users WHERE id = ?', (user_id,))
            user = c.fetchone()
            conn.close()
            if user and hashlib.sha256(password.encode()).hexdigest() == user[0]:
                conn = sqlite3.connect(DB_PATH)
                c = conn.cursor()
                c.execute('UPDATE purchases SET verified = 1 WHERE purchase_token = ?', (purchase_token,))
                conn.commit()
                conn.close()
                flash('验证成功！')
                return redirect(url_for('dashboard'))
            else:
                flash('密码错误，请重试')
    return render_template('verify_purchase.html')

# ============================================================
# 管理员后台（产品管理）
# ============================================================
@app.route('/admin/products')
@admin_required
def admin_products():
    if IS_PRODUCTION:
        products = Product.query.all()
    else:
        conn = sqlite3.connect(DB_PATH)
        c = conn.cursor()
        c.execute('SELECT id, name, description, price, currency, is_active, created_at FROM products ORDER BY id')
        products = c.fetchall()
        conn.close()
    return render_template('admin_products.html', products=products)

@app.route('/admin/products/add', methods=['GET', 'POST'])
@admin_required
def add_product():
    if request.method == 'POST':
        name = request.form['name'].strip()
        description = request.form['description'].strip()
        price = float(request.form['price'])
        currency = request.form['currency']
        if not name or price <= 0:
            flash('请正确填写产品信息')
            return render_template('admin_product_form.html', action='添加')
        now = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        if IS_PRODUCTION:
            product = Product(name=name, description=description, price=price, currency=currency, is_active=1, created_at=now)
            db.session.add(product)
            db.session.commit()
        else:
            conn = sqlite3.connect(DB_PATH)
            c = conn.cursor()
            c.execute('''
                INSERT INTO products (name, description, price, currency, is_active, created_at)
                VALUES (?, ?, ?, ?, 1, ?)
            ''', (name, description, price, currency, now))
            conn.commit()
            conn.close()
        flash('产品添加成功')
        return redirect(url_for('admin_products'))
    return render_template('admin_product_form.html', action='添加')

@app.route('/admin/products/edit/<int:product_id>', methods=['GET', 'POST'])
@admin_required
def edit_product(product_id):
    if request.method == 'POST':
        name = request.form['name'].strip()
        description = request.form['description'].strip()
        price = float(request.form['price'])
        currency = request.form['currency']
        is_active = 1 if request.form.get('is_active') == 'on' else 0
        if not name or price <= 0:
            flash('请正确填写产品信息')
            if IS_PRODUCTION:
                product = Product.query.get(product_id)
            else:
                conn = sqlite3.connect(DB_PATH)
                c = conn.cursor()
                c.execute('SELECT id, name, description, price, currency, is_active FROM products WHERE id = ?', (product_id,))
                product = c.fetchone()
                conn.close()
            return render_template('admin_product_form.html', action='编辑', product=product)
        if IS_PRODUCTION:
            product = Product.query.get(product_id)
            if product:
                product.name = name
                product.description = description
                product.price = price
                product.currency = currency
                product.is_active = is_active
                db.session.commit()
        else:
            conn = sqlite3.connect(DB_PATH)
            c = conn.cursor()
            c.execute('''
                UPDATE products SET name=?, description=?, price=?, currency=?, is_active=?
                WHERE id=?
            ''', (name, description, price, currency, is_active, product_id))
            conn.commit()
            conn.close()
        flash('产品更新成功')
        return redirect(url_for('admin_products'))
    else:
        if IS_PRODUCTION:
            product = Product.query.get(product_id)
            if not product:
                flash('产品不存在')
                return redirect(url_for('admin_products'))
            product = (product.id, product.name, product.description, product.price, product.currency, product.is_active)
        else:
            conn = sqlite3.connect(DB_PATH)
            c = conn.cursor()
            c.execute('SELECT id, name, description, price, currency, is_active FROM products WHERE id = ?', (product_id,))
            product = c.fetchone()
            conn.close()
            if not product:
                flash('产品不存在')
                return redirect(url_for('admin_products'))
        return render_template('admin_product_form.html', action='编辑', product=product)

@app.route('/admin/products/delete/<int:product_id>', methods=['POST'])
@admin_required
def delete_product(product_id):
    if IS_PRODUCTION:
        product = Product.query.get(product_id)
        if product:
            db.session.delete(product)
            db.session.commit()
    else:
        conn = sqlite3.connect(DB_PATH)
        c = conn.cursor()
        c.execute('DELETE FROM products WHERE id = ?', (product_id,))
        conn.commit()
        conn.close()
    flash('产品已删除')
    return redirect(url_for('admin_products'))

# ============================================================
# 启动服务器
# ============================================================
if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=False)
