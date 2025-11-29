import { useNavigate } from 'react-router-dom';
import { Smartphone, Shirt, Home, Dumbbell, TrendingDown, Search, Zap } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import DashboardNav from '@/components/DashboardNav';
import Footer from '@/components/Footer';
import { useState } from 'react';
import { useAuth } from '@/hooks/useAuth';

const Dashboard = () => {
  const navigate = useNavigate();
  const [searchQuery, setSearchQuery] = useState('');
  const { userProfile } = useAuth();
  const userName = userProfile?.name || 'Usuário';

  const handleSearch = (query: string) => {
    if (query.trim()) {
      navigate(`/resultados?q=${encodeURIComponent(query)}`);
    }
  };

  const categories = [
  {
    id: 'electronics',
    name: 'Eletrônicos',
    searchTerm: 'celular',
    icon: Smartphone,
    color: 'bg-primary',
  },
  {
    id: 'fashion',
    name: 'Moda',
    searchTerm: 'moda',
    icon: Shirt,
    color: 'bg-[#a855f7]',
  },
  {
    id: 'home',
    name: 'Casa',
    searchTerm: 'decoracao',
    icon: Home,
    color: 'bg-[#22c55e]',
  },
  {
    id: 'sports',
    name: 'Esportes',
    searchTerm: 'esporte',
    icon: Dumbbell,
    color: 'bg-[#f97316]',
  },
];


  const quickCategories = ['Smartphones', 'Notebooks', 'TVs', 'Roupas', 'Tênis', 'Livros'];

  const todayDeals = [
    {
      name: 'iPhone 15 Pro 256GB',
      oldPrice: 7999,
      newPrice: 6499,
      discount: 19,
      store: 'Amazon',
    },
    {
      name: 'Samsung Galaxy S24 Ultra',
      oldPrice: 8999,
      newPrice: 7299,
      discount: 19,
      store: 'Magazine Luiza',
    },
    {
      name: 'Notebook Dell Inspiron i15',
      oldPrice: 4299,
      newPrice: 3199,
      discount: 26,
      store: 'Mercado Livre',
    },
    {
      name: 'Smart TV LG 55\' 4K',
      oldPrice: 3499,
      newPrice: 2699,
      discount: 23,
      store: 'Casas Bahia',
    },
  ];

  return (
    <div className='min-h-screen flex flex-col bg-background'>
      <DashboardNav onSearch={handleSearch} userName={userName} />

      {/* Hero Section */}
      <section className='py-12 px-4 sm:px-6 lg:px-8 bg-gradient-hero'>
        <div className='container mx-auto'>
          <div className='max-w-4xl mx-auto text-center'>
            <h1 className='text-3xl sm:text-4xl md:text-5xl font-bold text-foreground mb-4 animate-fade-in-up'>
              Olá, {userName}! 👋
              <br />
              <span className='gradient-text'>O que você procura hoje?</span>
            </h1>
            <p className='text-lg text-muted-foreground mb-8 animate-fade-in-up'>
              Encontre os melhores preços em milhares de produtos
            </p>

            {/* Main Search */}
            <form
              onSubmit={(e) => {
                e.preventDefault();
                handleSearch(searchQuery);
              }}
              className='max-w-2xl mx-auto mb-6 animate-fade-in-up'
            >
              <div className='relative'>
                <Search className='absolute left-4 top-1/2 -translate-y-1/2 h-6 w-6 text-muted-foreground' />
                <Input
                  type='search'
                  placeholder='Busque produtos, marcas ou categorias...'
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className='pl-14 pr-4 h-16 text-lg shadow-lg'
                />
                <Button
                  type='submit'
                  variant='gradient'
                  size='lg'
                  className='absolute right-2 top-1/2 -translate-y-1/2'
                >
                  Buscar
                </Button>
              </div>
            </form>

            {/* Quick Categories */}
            <div className='flex flex-wrap justify-center gap-2 animate-fade-in-up'>
              {quickCategories.map((cat) => (
                <Button
                  key={cat}
                  variant='outline'
                  size='sm'
                  onClick={() => handleSearch(cat)}
                  className='rounded-full'
                >
                  {cat}
                </Button>
              ))}
            </div>
          </div>
        </div>
      </section>

          {/* Favorite Categories */}
    <section className='py-12 px-4 sm:px-6 lg:px-8 bg-background'>
      <div className='container mx-auto'>
        <h2 className='text-2xl sm:text-3xl font-bold text-foreground mb-6'>
          Suas Categorias Favoritas
        </h2>

        <div className='grid grid-cols-2 lg:grid-cols-4 gap-4'>
          {categories.map((category) => (
            <Card
              key={category.id}
              className='glass hover-lift cursor-pointer group'
              onClick={() =>
                navigate(`/resultados?q=${encodeURIComponent(category.searchTerm)}`)
              }
            >
              <CardContent className='p-6 text-center'>
                <div
                  className={`w-16 h-16 rounded-xl ${category.color} flex items-center justify-center mx-auto mb-4 shadow-lg group-hover:shadow-glow transition-all`}
                >
                  <category.icon className='h-8 w-8 text-white' />
                </div>
                <h3 className='font-semibold text-foreground mb-2'>
                  {category.name}
                </h3>
                <Button
                  variant='ghost'
                  size='sm'
                  className='text-primary'
                  onClick={(e) => {
                    e.stopPropagation();
                    navigate(`/resultados?q=${encodeURIComponent(category.searchTerm)}`);
                  }}
                >
                  Ver ofertas →
                </Button>
              </CardContent>
            </Card>
          ))}
        </div>
      </div>
    </section>


      {/* Today's Deals */}
      <section className='py-12 px-4 sm:px-6 lg:px-8 bg-gradient-subtle'>
        <div className='container mx-auto'>
          <div className='flex items-center justify-between mb-6'>
            <h2 className='text-2xl sm:text-3xl font-bold text-foreground flex items-center gap-2'>
              <Zap className='h-7 w-7 text-accent' />
              Ofertas do Dia
            </h2>
            <Button variant='outline' onClick={() => handleSearch('ofertas')}>
              Ver todas
            </Button>
          </div>

          <div className='grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4'>
            {todayDeals.map((deal, index) => (
              <Card key={index} className='glass hover-lift cursor-pointer group'>
                <CardContent className='p-0'>
                  {/* Product Image Placeholder */}
                  <div className='h-48 bg-gradient-subtle rounded-t-lg flex items-center justify-center'>
                    <Smartphone className='h-20 w-20 text-primary/40 group-hover:text-primary/60 transition-colors' />
                  </div>

                  {/* Product Info */}
                  <div className='p-4'>
                    <div className='flex items-start justify-between mb-2'>
                      <h3 className='font-semibold text-sm leading-tight flex-1'>{deal.name}</h3>
                      <Badge variant='destructive' className='ml-2'>
                        -{deal.discount}%
                      </Badge>
                    </div>

                    <div className='mb-3'>
                      <div className='text-xs text-muted-foreground line-through'>
                        {deal.oldPrice.toLocaleString('pt-BR', {
                          style: 'currency',
                          currency: 'BRL',
                        })}
                      </div>
                      <div className='text-xl font-bold text-accent'>
                        {deal.newPrice.toLocaleString('pt-BR', {
                          style: 'currency',
                          currency: 'BRL',
                        })}
                      </div>
                      <div className='text-xs text-muted-foreground mt-1'>em {deal.store}</div>
                    </div>

                    <Button variant='gradient' size='sm' className='w-full'>
                      Ver Ofertas
                    </Button>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        </div>
      </section>

      {/* How It Works */}
      <section className='py-12 px-4 sm:px-6 lg:px-8 bg-background'>
        <div className='container mx-auto'>
          <h2 className='text-2xl sm:text-3xl font-bold text-foreground text-center mb-12'>
            Como Funciona
          </h2>

          <div className='grid grid-cols-1 md:grid-cols-3 gap-8 max-w-4xl mx-auto'>
            <div className='text-center'>
              <div className='w-16 h-16 rounded-full bg-gradient-primary flex items-center justify-center mx-auto mb-4 shadow-glow'>
                <Search className='h-8 w-8 text-white' />
              </div>
              <h3 className='font-semibold text-lg mb-2'>1. Busque o produto</h3>
              <p className='text-muted-foreground'>
                Digite o que você procura e nossa IA encontra as melhores opções
              </p>
            </div>

            <div className='text-center'>
              <div className='w-16 h-16 rounded-full bg-secondary flex items-center justify-center mx-auto mb-4 shadow-lg'>
                <TrendingDown className='h-8 w-8 text-white' />
              </div>
              <h3 className='font-semibold text-lg mb-2'>2. Compare preços</h3>
              <p className='text-muted-foreground'>
                Veja ofertas de centenas de lojas lado a lado em segundos
              </p>
            </div>

            <div className='text-center'>
              <div className='w-16 h-16 rounded-full bg-accent flex items-center justify-center mx-auto mb-4 shadow-lg'>
                <Badge className='h-8 w-8 bg-transparent'>💰</Badge>
              </div>
              <h3 className='font-semibold text-lg mb-2'>3. Economize dinheiro</h3>
              <p className='text-muted-foreground'>
                Escolha a melhor oferta e economize até 40% nas suas compras
              </p>
            </div>
          </div>
        </div>
      </section>

      <Footer />
    </div>
  );
};

export default Dashboard;
